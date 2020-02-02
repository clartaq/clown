(ns clown.client.core
  (:require [cljs.core.async :refer [chan close! <! >!]]
            [cljs.tools.reader.edn :as edn]
            [clojure.string :as s]
            [clown.client.commands :as cmd]
            [clown.client.dialogs.ok-dialogs :as dlg]
            [clown.client.layout :as ay]
            [clown.client.tree-ids :as ti]
            [clown.client.tree-manip :as tm]
            [clown.client.util.dom-utils :as du]
            [clown.client.util.empty-outline :refer [build-empty-outline
                                                     empty-outline-file-name
                                                     formatted-time-now]]
            [clown.client.util.focus-utils :as fu]
            [clown.client.util.marker :as mrk]
            [clown.client.util.mru :refer [push-on-mru!]]
            [clown.client.util.undo-redo :as ur]
            [clown.client.util.vector-utils :refer [delete-at remove-first
                                                    remove-last remove-last-two
                                                    insert-at replace-at
                                                    append-element-to-vector]]
            [clown.client.ws :refer [start-ws!]]
            [reagent.core :as r]
            [reagent.dom.server :refer [render-to-string render-to-static-markup]]
            [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                                trace debug info warn error]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^{:private true} my-global-state-ratom-with-a-terribly-long-name-so-i-dont-use-it-anywhere-else
  (r/atom {:program-name      "clown"
           :user              "unknown"
           :num-outliner-rows 0
           :num-note-chars    0
           :num-note-words    0
           :current-outline   {:version          "0.0.1"
                               :author           "Unknown"
                               :title            "Unknown"
                               :created          nil
                               :modified         nil
                               :focused-headline nil
                               :tree             [{:topic "Your outline here"}]}}))

(defn state-ratom
  "Return an atom containing the global program state."
  []
  my-global-state-ratom-with-a-terribly-long-name-so-i-dont-use-it-anywhere-else)

;;;-----------------------------------------------------------------------------
;;; Global Data and Constants

;; A channel used to retrieve the preferences asynchronously when it becomes
;; available from the websocket.
(def ^{:private true} got-prefs-channel (chan))

;;;-----------------------------------------------------------------------------
;;; Working with WebSockets.

(defn ws-message-handler
  "Handle messages received from the server."
  [e]
  (debug "ws-message-handler: Saw a message come in to the client.")
  (debugf "(du/event-data e): %s" (du/event-data e))
  (let [message-map (edn/read-string (du/event-data e))
        message-command (get-in message-map [:message :command])
        message-data (get-in message-map [:message :data])]
    (debugf "message-command: %s" message-command)
    (debugf "message-data: %s" message-data)

    (cond

      (= message-command "hey-client/no-such-file")
      (do
        (println "hey-client/no-such-file: message-data: " message-data)
        (dlg/toggle-file-does-not-exist-modal message-data))

      (= message-command "hey-client/accept-these-preferences")
      (do
        (debug "Saw message-command \"hey-client/accept-these-preferences\"")
        ;; On reload, the initial render function is waiting on the
        ;; preferences before it lays out the page since the layout
        ;; is dependent on items stored in the preferences.
        (debugf "message-data: %s" message-data)
        (go (>! got-prefs-channel message-data)))

      (= message-command "hey-client/accept-user-name")
      (swap! (state-ratom) assoc :user message-data)

      (= message-command "hey-client/accept-this-outline")
      (do
        (infof "Saw message-command \"hey-client/accept-this-outline\"")
        (infof "(:outline message-data): %s" (:outline message-data))
        (swap! (state-ratom) assoc :current-outline (:outline message-data))
        (infof "@(state-ratom): %s" @(state-ratom))))))

;;;-----------------------------------------------------------------------------
;;; Functions to handle keystroke events. Editing commands.
;;;

(defn delete-one-character-backward!
  "Handle the special case where the current headline has no more characters.
  Delete it and any children, then move the editor focus to the headline
  above it. Will not delete the last remaining top-level headline."
  [{:keys [aps root-ratom evt topic-ratom span-id]} & [caret-pos]]
  (when (zero? (count @topic-ratom))
    (du/prevent-default evt)
    (let [previous-visible-topic-id (tm/previous-visible-node root-ratom span-id)]
      (when-let [previous-topic-value (tm/get-topic root-ratom previous-visible-topic-id)]
        (mrk/mark-as-dirty! aps)
        (let [caret-position (or caret-pos (count (:topic previous-topic-value)))
              previous-visible-editor-id (ti/change-tree-id-type previous-visible-topic-id "editor")]
          (tm/prune-topic! root-ratom span-id)
          (when (du/get-element-by-id previous-visible-editor-id)
            (fu/focus-and-scroll-editor-for-id previous-visible-topic-id caret-position)))))))

(defn delete-one-character-forward!
  "Handle the special case where there are no more characters in the headline.
  In that case the headline will be deleted and the focus will move to the
  previous visible node. Will not delete the last remaining top-level node."
  [{:keys [aps root-ratom evt topic-ratom span-id] :as args}]
  (when (zero? (count @topic-ratom))
    (du/prevent-default evt)
    (if-let [children (tm/children? root-ratom span-id)]
      (do
        (mrk/mark-as-dirty! aps)
        (when (tm/expanded? root-ratom span-id)
          (tm/outdent-all-children! root-ratom span-id children))
        (tm/prune-topic! root-ratom span-id)
        ;; Did we delete all of the children that might have advanced to the
        ;; same id as span-id. That is, was it the last visible branch in the
        ;; tree?
        (let [node-to-focus (if (ti/lower? span-id (tm/last-node-in-tree root-ratom))
                              (tm/last-visible-node-in-tree root-ratom)
                              span-id)]
          (r/after-render #(fu/focus-and-scroll-editor-for-id node-to-focus 0))))
      ;; else
      (if-let [next-topic-id (if-not (empty? (tm/siblings-below root-ratom span-id))
                               span-id
                               (tm/next-visible-node root-ratom span-id))]
        (let [next-topic-editor-id (ti/change-tree-id-type next-topic-id "editor")]
          (mrk/mark-as-dirty! aps)
          (tm/prune-topic! root-ratom span-id)
          (r/after-render #(fu/focus-and-scroll-editor-for-id next-topic-editor-id 0)))
        (do
          (mrk/mark-as-dirty! aps)
          (delete-one-character-backward! args 0))))))

(defn indent!
  "Indent the current headline one level."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (let [editor-id (ti/change-tree-id-type span-id "editor")
        caret-position (du/get-caret-position editor-id)]
    (when-let [demoted-id (tm/indent-branch! root-ratom span-id)]
      (mrk/mark-as-dirty! aps)
      (r/after-render #(fu/focus-and-scroll-editor-for-id demoted-id caret-position)))))

(defn outdent!
  "Outdent the current headline one level."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (let [editor-id (ti/change-tree-id-type span-id "editor")
        caret-position (du/get-caret-position editor-id)]
    (when-let [promoted-id (tm/outdent-branch! root-ratom span-id)]
      (mrk/mark-as-dirty! aps)
      (println "outdent! dirty?: " (mrk/dirty? aps))
      (r/after-render #(fu/focus-and-scroll-editor-for-id promoted-id caret-position)))))

(defn move-headline-up!
  "Move the current headline up one position in its group of siblings."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (let [siblings-above (tm/siblings-above root-ratom span-id)]
    (when (pos? (count siblings-above))
      (let [editor-id (ti/change-tree-id-type span-id "editor")
            caret-position (du/get-caret-position editor-id)
            new-id (first siblings-above)
            new-editor-id (ti/change-tree-id-type new-id "editor")]
        (mrk/mark-as-dirty! aps)
        (tm/move-branch! root-ratom span-id new-id)
        (fu/focus-and-scroll-editor-for-id new-editor-id caret-position)))))

(defn move-headline-down!
  "Move the current headline down one position in its group of siblings."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (let [siblings-below (tm/siblings-below root-ratom span-id)]
    (when (pos? (count siblings-below))
      (let [editor-id (ti/change-tree-id-type span-id "editor")
            caret-position (du/get-caret-position editor-id)
            new-id (ti/increment-leaf-index-by span-id 2)
            new-editor-id (ti/change-tree-id-type (ti/increment-leaf-index span-id) "editor")]
        (mrk/mark-as-dirty! aps)
        (tm/move-branch! root-ratom span-id new-id)
        (fu/focus-and-scroll-editor-for-id new-editor-id caret-position)))))

(defn move-focus-up-one-line
  "Move the editor and focus to the next higher up visible headline."
  [{:keys [root-ratom evt span-id]}]
  (du/prevent-default evt)
  (when-not (tm/top-visible-tree-id? root-ratom span-id)
    (let [editor-id (ti/change-tree-id-type span-id "editor")
          saved-caret-position (du/selection-start editor-id)
          previous-visible-topic (tm/previous-visible-node root-ratom span-id)]
      (fu/focus-and-scroll-editor-for-id previous-visible-topic saved-caret-position))))

(defn move-focus-down-one-line
  "Move the editor and focus to the next lower down visible headline."
  [{:keys [root-ratom evt span-id]}]
  (du/prevent-default evt)
  (when-not (tm/bottom-visible-tree-id? root-ratom span-id)
    (let [editor-id (ti/change-tree-id-type span-id "editor")
          saved-caret-position (du/selection-start editor-id)
          next-visible-topic (tm/next-visible-node root-ratom span-id)]
      (fu/focus-and-scroll-editor-for-id next-visible-topic saved-caret-position))))

(defn insert-new-headline-below!
  "Insert a new headline in the tree above the currently focused one and leave
   the placeholder text highlighted ready to be overwritten when the user
   starts typing."
  [{:keys [aps root-ratom evt span-id]}]
  ;; If the topic span has children, add a new child in the zero-position
  ;; Else add a new sibling below the current topic
  (du/prevent-default evt)
  (let [id-of-new-child (if (tm/expanded? root-ratom span-id)
                          (ti/insert-child-index-into-parent-id span-id 0)
                          (ti/increment-leaf-index span-id))
        new-headline (ti/new-topic)
        num-chars (count (:topic new-headline))]
    (mrk/mark-as-dirty! aps)
    (tm/graft-topic! root-ratom id-of-new-child new-headline)
    (r/after-render #(fu/highlight-and-scroll-editor-for-id id-of-new-child 0 num-chars))))

(defn insert-new-headline-above!
  "Insert a new headline above the current headline, pushing the current
  headline down. Leave the new topic placeholder text highlighted ready to
  be overwritten when the user starts typing."
  [{:keys [aps root-ratom evt span-id]}]
  (println "insert-new-headline-above!")
  (du/prevent-default evt)
  (let [new-headline (ti/new-topic)
        num-chars (count (:topic new-headline))]
    (mrk/mark-as-dirty! aps)
    (tm/graft-topic! root-ratom span-id new-headline)
    (r/after-render #(fu/highlight-and-scroll-editor-for-id span-id 0 num-chars))))

(defn delete-branch!
  "Delete the branch specified, including all of its children."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (mrk/mark-as-dirty! aps)
  (tm/prune-topic! root-ratom span-id))

(defn split-headline!
  "Split the headline at the caret location. Text to the left of the caret
  will remain at the existing location. Text to the right of the caret (and
  any children) will appear as a new sibling branch below the existing
  headline."
  ;; I can make different arguments for whether the caret should be left at
  ;; the end of the top headline or moved to the beginning of the new branch.
  ;; Went with top headline for now.
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (when-let [sel-end (du/selection-end (ti/change-tree-id-type span-id "editor"))]
    (let [existing-topic (tm/get-topic root-ratom span-id)
          topic-text (:topic existing-topic)
          text-before (s/trimr (s/join (take sel-end topic-text)))
          cnt (count text-before)
          text-after (s/triml (s/join (drop sel-end topic-text)))
          headline-above {:topic text-before}
          branch-below (assoc existing-topic :topic text-after)]
      (mrk/mark-as-dirty! aps)
      (tm/prune-topic! root-ratom span-id)
      (tm/graft-topic! root-ratom span-id branch-below)
      (tm/graft-topic! root-ratom span-id headline-above)
      (r/after-render
        #(fu/focus-and-scroll-editor-for-id span-id cnt)))))

(defn join-headlines!
  "Joins the current headline with the sibling branch below it."
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (let [id-below (ti/increment-leaf-index span-id)]
    (when-let [branch-below (tm/get-topic root-ratom id-below)]
      (let [top-topic (tm/get-topic root-ratom span-id)
            cnt (count top-topic)
            new-headline (str (:topic top-topic) " " (:topic branch-below))
            with-new-headline (assoc top-topic :topic new-headline)
            with-children (if (or (:children top-topic) (:children branch-below))
                            (let [exp-state (or (:expanded top-topic) (:expanded branch-below))
                                  startv (or (:children top-topic) [])
                                  children (into startv (map identity (:children branch-below)))]
                              (-> with-new-headline
                                  (assoc :children children)
                                  (assoc :expanded exp-state)))
                            with-new-headline)]
        (mrk/mark-as-dirty! aps)
        (tm/prune-topic! root-ratom span-id)
        (tm/prune-topic! root-ratom span-id)
        (tm/graft-topic! root-ratom span-id with-children)
        (fu/focus-and-scroll-editor-for-id span-id cnt)))))

(defn toggle-headline-expansion!
  "Toggle the expansion state of the current headline."
  [{:keys [aps root-ratom evt span-id]}]
  (println "toggle-headline-expansion!" toggle-headline-expansion!)
  (du/prevent-default evt)
  (mrk/mark-as-dirty! aps)
  (println "(mrk/dirty? root-ratom)" (mrk/dirty? root-ratom))
  (tm/toggle-node-expansion! root-ratom span-id))

(defn expand-headline!
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (mrk/mark-as-dirty! aps)
  (tm/expand-node! root-ratom span-id))

(defn collapse-headline!
  [{:keys [aps root-ratom evt span-id]}]
  (du/prevent-default evt)
  (mrk/mark-as-dirty! aps)
  (tm/collapse-node! root-ratom span-id))

(defn- expand-from-parts!
  [root-ratom parts-vector]
  (let [id (ti/nav-index-vector->tree-id-string parts-vector)]
    (when (and (tm/children? root-ratom id) (not (tm/expanded? root-ratom id)))
      (tm/expand-node! root-ratom id))))

(defn expand-all-branches!
  "Expand all headlines."
  [{:keys [aps root-ratom evt]}]
  (trace "expand-all-branches!")
  (mapv #(expand-from-parts! root-ratom %) (tm/all-nodes @root-ratom []))
  (mrk/mark-as-dirty! aps)
  (du/prevent-default evt))

(defn- collapse-from-parts!
  [root-ratom parts-vector]
  (let [id (ti/nav-index-vector->tree-id-string parts-vector)]
    (when (and (tm/children? root-ratom id) (tm/expanded? root-ratom id))
      (tm/collapse-node! root-ratom id))))

(defn collapse-all-branches!
  "Collapse all branches."
  [{:keys [aps root-ratom evt]}]
  (trace "collapse-all-branches!")
  (mapv #(collapse-from-parts! root-ratom %) (tm/all-nodes @root-ratom []))
  (mrk/mark-as-dirty! aps)
  (du/prevent-default evt))

(defn- def-mods
  "Return a map containing the default values for keyboard modifiers."
  []
  {:ctrl false :alt false :shift false :cmd false})

(defn- merge-def-mods
  "Merge a map of modifiers (containing any modifiers which should be present)
  with a default map of false values for all modifiers."
  [m]
  (merge (def-mods) m))

(defn handle-key-down-for-outline
  "Handle key-down events and dispatch them to the appropriate handlers."
  [aps root-ratom evt topic-ratom span-id]
  (let [km (du/key-evt->map evt)
        args {:aps         aps
              :root-ratom  root-ratom
              :evt         evt
              :topic-ratom topic-ratom
              :span-id     span-id}]
    (debugf "km: %s" km)
    (cond

      (= km {:key "Enter" :modifiers (merge-def-mods {:shift true})})
      (insert-new-headline-above! args)

      (= km {:key "Enter" :modifiers (def-mods)})
      (insert-new-headline-below! args)

      (= km {:key "Enter" :modifiers (merge-def-mods {:ctrl true})})
      (split-headline! args)

      (= km {:key "Enter" :modifiers (merge-def-mods {:ctrl true :shift true})})
      (join-headlines! args)

      (= km {:key "k" :modifiers (merge-def-mods {:cmd true})})
      (delete-branch! args)

      (= km {:key "Delete" :modifiers (def-mods)})
      (delete-one-character-forward! args)

      (= km {:key "Backspace" :modifiers (def-mods)})
      (delete-one-character-backward! args)

      (= km {:key "Tab" :modifiers (merge-def-mods {:shift true})})
      (outdent! args)

      (= km {:key "Tab" :modifiers (def-mods)})
      (indent! args)

      (= km {:key "ArrowUp" :modifiers (merge-def-mods {:alt true :cmd true})})
      (move-headline-up! args)

      (= km {:key "ArrowDown" :modifiers (merge-def-mods {:alt true :cmd true})})
      (move-headline-down! args)

      (= km {:key "ArrowUp" :modifiers (def-mods)})
      (move-focus-up-one-line args)

      (= km {:key "ArrowDown" :modifiers (def-mods)})
      (move-focus-down-one-line args)

      (= km {:key "0" :modifiers (merge-def-mods {:cmd true})})
      (expand-headline! args)

      (= km {:key "9" :modifiers (merge-def-mods {:cmd true})})
      (collapse-headline! args)

      ;; Option-Command-, despite what the :key looks like
      (= km {:key "≤" :modifiers (merge-def-mods {:cmd true :alt true})})
      (toggle-headline-expansion! args)

      ;; Shift-Option-Command-, despite what the :key looks like
      (= km {:key "¯" :modifiers (merge-def-mods {:shift true :cmd true :alt true})})
      (expand-all-branches! args)

      ;; Ctrl-Shift-Alt-,
      (= km {:key "<" :modifiers (merge-def-mods {:ctrl true :shift true :alt true})})
      (collapse-all-branches! args)

      :default nil)))

(defn handle-keydown-for-tree-container
  "Handle undo!/redo! for the tree container."
  [aps evt root-ratom um]
  (let [km (du/key-evt->map evt)]
    (cond
      (= km {:key "z" :modifiers (merge-def-mods {:cmd true})})
      (do
        (du/prevent-default evt)
        (when (ur/can-undo? um)
          (let [active-ele-id (du/active-element-id)]
            (ur/undo! um)
            (mrk/mark-as-dirty! aps)
            (when-not (or (ti/summit-id? active-ele-id)
                          (tm/expanded? root-ratom (ti/tree-id->parent-id active-ele-id)))
              (fu/focus-and-scroll-editor-for-id (tm/previous-visible-node root-ratom active-ele-id))))))

      (= km {:key "z" :modifiers (merge-def-mods {:cmd true :shift true})})
      (do
        (du/prevent-default evt)
        (when (ur/can-redo? um)
          (let [active-ele-id (du/active-element-id)]
            (ur/redo! um)
            (mrk/mark-as-dirty! aps)
            (when-not (or (ti/summit-id? active-ele-id)
                          (tm/expanded? root-ratom (ti/tree-id->parent-id active-ele-id)))
              (fu/focus-and-scroll-editor-for-id (tm/previous-visible-node root-ratom active-ele-id))))))

      :default nil)))

(defn capture-global-shortcuts
  "Capture keyboard shortcuts that apply anywhere in the app, regardless of
  what is, or is not, focused."
  [aps]
  (let [real-capture-fn (fn [evt aps]
                          (let [km (du/key-evt->map evt)]
                            (cond

                              ;; Load a minimal, new outline with Cmd-N.
                              (= km {:key "n" :modifiers (merge-def-mods {:cmd true})})
                              (do
                                (du/prevent-default evt)
                                (du/stop-propagation evt)
                                (println "Saw command to start new outline.")
                                (du/prevent-default evt))

                              ;; Open an existing outline with Cmd-O.
                              (= km {:key "o" :modifiers (merge-def-mods {:cmd true})})
                              (do
                                (du/prevent-default evt)
                                (du/stop-propagation evt)
                                (println "Saw command to open outline."))

                              ;; Save the current outline with Cmd-S.
                              (= km {:key "s" :modifiers (merge-def-mods {:cmd true})})
                              (do
                                (du/prevent-default evt)
                                (du/stop-propagation evt)
                                (cmd/save-outline-as-edn! aps))

                              ;; Open the preferences with Ctrl-Shft-P.
                              (= km {:key "P" :modifiers (merge-def-mods {:ctrl true :shift true})})
                              (do (du/prevent-default evt)
                                  (du/stop-propagation evt)
                                  (let [working-copy (r/atom (:preferences @aps))
                                        original-values @working-copy]
                                    (swap! aps assoc :working-copy working-copy)
                                    (swap! aps assoc :original-values original-values))
                                  (swap! aps assoc :show-prefs-dialog true)
                                  (r/after-render #(du/focus-element (du/get-element-by-id "pref-dialog-cancel-button-id"))))

                              ;; Open the not-saved for testing with Ctrl-Shft-A.
                              (= km {:key "A" :modifiers (merge-def-mods {:ctrl true :shift true})})
                              (do (du/prevent-default evt)
                                  (du/stop-propagation evt)
                                  (swap! aps assoc :show-not-saved-dialog true))


                              :default nil)))]
    (du/add-event-listener
      "keydown"
      (fn [evt]
        (real-capture-fn evt aps)))))

(defn on-ws-open
  "Handle the notification that the WebSocket has been opened."
  []
  (debug "on-ws-open")
  ((:send-message-fn @(state-ratom))
   (pr-str {:message {:command "hey-server/send-preferences"
                      :data    ""}})))

(defn reload []
  (timbre/set-level! :info)
  (info "reload")
  (capture-global-shortcuts (state-ratom))
  (swap! (state-ratom) assoc :outline-key-down-handler handle-key-down-for-outline)
  (swap! (state-ratom) assoc :outline-container-key-down-handler handle-keydown-for-tree-container)
  (let [ws-functions (start-ws! {:on-open-fn    on-ws-open
                                 :on-error-fn   nil
                                 :on-message-fn ws-message-handler})]
    (swap! (state-ratom) merge ws-functions)
    (debugf "    @(state-ratom): %s" @(state-ratom))

    ;; Wait for the preferences to be obtained from the server since they will
    ;; affect the rendering. Then, again depending on the preference,  get the
    ;; last file edited, if any.
    (go
      (let [prefs (<! got-prefs-channel)]
        (debugf "prefs: %s" prefs)
        (swap! (state-ratom) assoc :preferences prefs)
        (close! got-prefs-channel)
        (swap! (state-ratom) assoc :save-doc-fn cmd/save-outline-as-edn!)

        (if (:load-last-file prefs)
          (when-let [last-file (first (:mru prefs))]
            (debugf "Asking server to send last file: %s" last-file)
            ((:send-message-fn @(state-ratom))
             (pr-str {:message {:command "hey-server/send-outline"
                                :data    last-file}})))

          (do
            (swap! (state-ratom) assoc :current-outline (build-empty-outline (state-ratom)))
            (mrk/mark-as-clean! (state-ratom))
            (push-on-mru! (state-ratom) (empty-outline-file-name))))

        (r/render [ay/layout-app (state-ratom)]
                  (du/get-element-by-id "app"))))))

(defn ^:export main []
  (info "clown.core.main")
  (reload))

(defn init! [] (reload))

(reload)