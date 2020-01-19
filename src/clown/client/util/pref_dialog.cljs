(ns clown.client.util.pref-dialog
  (:require [clown.client.util.dom-utils :as du]
            ;[clown.client.util.ok-dialogs :as okd]
            [reagent.core :as r]))

(defn one-rem-spacer
  []
  [:label {:class "prefs--one-rem-spacer"} " "])

(defn half-rem-spacer
  []
  [:label {:class "prefs--half-rem-spacer"} " "])

(defn quarter-rem-spacer
  []
  [:label {:class "prefs--quarter-rem-spacer"} " "])

(defn id-basis->id
  [basis]
  (str basis "-modal"))

(defn id-basis->cancel-id
  [basis]
  (str basis "-cancel-button-id"))

(defn- toggle-modal
  "Toggle the display state of the modal dialog with the given id."
  [ele-id]
  (let [close-button (du/get-element-by-id ele-id)
        overlay (du/get-element-by-id "modal-overlay")]
    (when (and close-button overlay)
      (du/toggle-classlist close-button "closed")
      (du/toggle-classlist overlay "closed"))))

(def confirm-cancel-basis "confirm-cancel-basis")

(defn toggle-confirm-cancel-modal
  "Called by the keyboard shortcut handler."
  [evt]
  ;(println "toggle-confirm-cancel-modal: action-fn: " action-fn)
  (when evt
    (println "need to recalc for showing")
    ;(reset! aps assoc :showing true)
    )
  (toggle-modal (id-basis->id confirm-cancel-basis)))

(def confirm-cancel-test-params
  {:id-basis                  confirm-cancel-basis
   :header                    "Preferences"
   :message-text              ["Confirm or Cancel this dialog."
                               "Press One of the Buttons Below"]
   :confirm-button-text       "Save"
   :cancel-button-text        "Cancel"
   :confirm-button-popup-text "Save all changes and close this dialog."
   :cancel-button-popup-text  "Abandon all changes and close this dialog."
   :toggle-fn                 toggle-confirm-cancel-modal
   :confirm-action-fn         (fn [aps]
                                (println "You CONFIRMED your choices!")
                                (swap! aps dissoc :showing)
                                (toggle-confirm-cancel-modal nil))
   :cancel-action-fn          (fn [aps]
                                (println "You CANCELED your choices!")
                                (swap! aps dissoc :showing)
                                (toggle-confirm-cancel-modal nil))})

(defn save-confirmed-changes!
  "Save preferences changes both in the local program state and on the server."
  [aps original-values working-copy-ratom]
  (println "save-confirmed-changes!")
  (let [pc (r/cursor aps [:preferences])
        _ (println "    pc: " pc)
        pref-keys (keys original-values)
        _ (println "pref-keys: " pref-keys)]
    (doseq [k pref-keys]
      (when (not= (k original-values) (k @working-copy-ratom))
        (println "    values for " k " differ")
        (println "       original-values: " (k original-values))
        (println "       working-copy:    " (k @working-copy-ratom))
        (swap! pc assoc k (k @working-copy-ratom))
        (println "       pc after swap!:  " pc)
        ((:send-message-fn @aps)
         (pr-str {:message {:command "hey-server/persist-numeric-preference-value"
                            :data    {:preference k
                                      :value      (k @working-copy-ratom)}}})))))
  (toggle-confirm-cancel-modal nil))

(defn preferences-dialog-component
  [aps settings]
  (println "\n\npreferences-dialog-component: about to set local state: aps: " aps)
  (let [original-values (r/atom (:preferences @aps))
        _ (println "   original-values: " original-values)
        working-copy (r/atom @original-values)
        _ (println "   working-copy: " working-copy)
        load-file-r (r/atom (:load-last-file @working-copy)) ;cursor working-copy [:load-last-file])
        _ (println "   load-file-r: " load-file-r)
        interval-r (r/atom (:autosave_interval @working-copy));  working-copy [:autosave_interval])
        _ (println "   interval-r: " interval-r)
        o-width-r (r/atom (:outline_width working-copy)) ; working-copy [:outline_width])
        _ (println "   o-width-r: " o-width-r)
        n-width-r (r/atom (:note_width working-copy)) ; working-copy [:note_width])
        _ (println "   n-width-r: " n-width-r)
        ]
    (r/create-class
      {
       :display-name "preferences-dialog-component"

       :component-will-update
                     (fn [] (println "component-will-update"))

       :component-did-mount
                     (fn []
                       (println "compnonent-did-mount")
                       ;(println "next-props: " next-props)
                       ;(println "next-state: " next-state)
                       (reset! original-values (:preferences @aps))
                       (println "   original-values: " original-values)
                       (reset! working-copy @original-values)
                       (println "   working-copy: " working-copy)
                       (reset! load-file-r (:load-last-file @working-copy)) ;(r/cursor working-copy [:load-last-file]))
                       (println "   load-file-r: " load-file-r)
                       (reset! interval-r (:autosave_interval @working-copy)) ;(r/cursor working-copy [:autosave_interval]))
                       (println "   interval-r: " interval-r)
                       (reset! o-width-r (:outline_width @working-copy)) ;r/cursor working-copy [:outline_width]))
                       (println "   o-width-r: " o-width-r)
                       (reset! n-width-r (:note_width @working-copy)); r/cursor working-copy [:note_width]))
                       (println "   n-width-r: " n-width-r))

       :reagent-render
                     (fn [aps settings]
                       [:div {:class "modal closed"
                              :id    (id-basis->id (:id-basis settings))
                              :role  "dialog"}
                        [:header {:class "modal-header"}
                         [:section {:class "modal-header-left"} (:header settings)]
                         [:section {:class "modal-header-right"}
                          [:input {:type     "button"
                                   :class    "tree-demo--button"
                                   :id       (str "close- " (:id-basis settings) "-button")
                                   :value    "Close"
                                   :title    (:cancel-button-popup-text settings)
                                   :on-click #((:cancel-action-fn settings) aps)}]]]

                        [:form {:class "modal-guts"}
                         [:field-set {:class "prefs--container"}

                          ;; The radio buttons to selection whether or not to load the last
                          ;; document viewed the next time the program is loaded.
                          [:div {:class "prefs--row"}
                           [:label {:class "prefs--label"} "Load Last Document:"]
                           ;[:input {:type      "text"
                           ;         :class     "prefs--text-editor"
                           ;         :value     (str (get-in @aps [:preferences :load-last-file]))
                           ;         :on-change #(println "value: ")}]

                           ;<label>Radio buttons</label>
                           ;<input type = "radio"
                           ;name = "radSize"
                           ;id = "sizeSmall"
                           ;value = "small"
                           ;checked = "checked" />
                           ;<label for = "sizeSmall">small</label>
                           ;<input type = "radio"
                           ;name = "radSize"
                           ;id = "sizeMed"
                           ;value = "medium" />
                           ;; See https://stackoverflow.com/questions/8838648/onchange-event-handler-for-radio-button-input-type-radio-doesnt-work-as-one
                           [:div {:class "prefs--radio-group"}
                            [:input {:type      "radio"
                                     :name      "load-last-file"
                                     :id        "do-load-last-file"
                                     :checked   (if @load-file-r
                                                  "checked"
                                                  nil)
                                     :on-change #(do (println "YES LOAD changed")
                                                     (if @load-file-r
                                                       ; Changing from don't want it to want it.
                                                       (do (println "want it now)"))
                                                       (do (println "don't want it"))))
                                     :value     "Yes"}]
                            (quarter-rem-spacer)
                            [:label {:for "do-load-last-file"} "Yes"]
                            (one-rem-spacer)

                            [:input {:type      "radio"
                                     :name      "load-last-file"
                                     :id        "do-not-load-last-file"
                                     :checked   (if @load-file-r ;(get-in @aps [:preferences :load-last-file])
                                                  nil
                                                  "checked")
                                     :on-change #(do (println "DONT LOAD changed")
                                                     (if @load-file-r
                                                       ; Changing from not wanting it to wanting it.
                                                       (do (println "Want it"))
                                                       (do (println "Don' want it"))))
                                     :value     "No"}]
                            (quarter-rem-spacer)
                            [:label {:for "do-not-load-last-file"} "No"]]
                           (half-rem-spacer)
                           [:label {:class "prefs--help-char"
                                    :title (str "Whether to load the last document viewed the "
                                                "next time the program is started.")} "?"]]

                          ;; Autosave interval.
                          [:div {:class "prefs--row"}
                           [:label {:class "prefs--label"} "Autosave Interval:"]
                           [:input {:type      "number"
                                    :class     "prefs--text-editor"
                                    :step      1
                                    :value     @interval-r
                                    :on-change #(reset! interval-r (js/parseInt (du/event->target-value %)))}]
                           (half-rem-spacer)
                           [:label {:class "prefs--help-char"
                                    :title (str "The number of idle seconds to wait for before "
                                                "saving the document. A value of 0 means no "
                                                "automatic saves will be done.")} "?"]]
                          [:p {:class "prefs--help-text"
                               :style {:color "red"}}
                           "The \"Autosave\" function is not ready yet."]

                          ;; Column width for outliner.
                          [:div {:class "prefs--row"}
                           [:label {:class "prefs--label"} "Outliner Column Width:"]
                           [:input {:type      "number"
                                    :class     "prefs--text-editor"
                                    :step      1
                                    :value     @o-width-r
                                    :on-change #(reset! o-width-r (js/parseInt (du/event->target-value %)))}]
                           (half-rem-spacer)
                           [:label {:class "prefs--help-char"
                                    :title (str "The width of the outliner column, in pixels. "
                                                "This can also be set by dragging the right border "
                                                "of the column with the mouse.")} "?"]]

                          ;; Column width for notes.
                          [:div {:class "prefs--row"}
                           [:label {:class "prefs--label"} "Note Column Width:"]
                           [:input {:type      "number"
                                    :class     "prefs--text-editor"
                                    :step      1
                                    :value     @n-width-r
                                    :on-change #(reset! n-width-r (js/parseInt (du/event->target-value %)))}]
                           (half-rem-spacer)
                           [:label {:class "prefs--help-char"
                                    :title (str "The width of the note column, in pixels. "
                                                "This can also be set by dragging the right border "
                                                "of the column with the mouse.")} "?"]]]]

                        ;; The footer with the cancel/confirm buttons.
                        [:div {:class "modal-footer"}
                         [:section {:class "tree-demo--button-area"}
                          [:input {:type     "button"
                                   :class    "tree-demo--button button-bar-item"
                                   :value    (:cancel-button-text settings)
                                   :title    (:cancel-button-popup-text settings)
                                   :on-click #((:cancel-action-fn settings) aps)}]
                          [:input {:type     "button"
                                   :class    "tree-demo--button button-bar-item"
                                   :id       (id-basis->cancel-id (:id-basis settings))
                                   :value    (:confirm-button-text settings)
                                   :title    (:confirm-button-popup-text settings)
                                   ;;;
                                   :on-click #(save-confirmed-changes! aps @original-values working-copy)
                                   ;;;
                                   :disabled (= @original-values @working-copy)}]]]])
       })))

(defn confirm-cancel-dialog-template
  "Return a function to lay out the preferences dialog."
  [aps settings]
  (let [original-values (:preferences @aps)
        working-copy (r/atom original-values)
        load-file-r (r/cursor working-copy [:load-last-file])
        interval-r (r/cursor working-copy [:autosave_interval])
        o-width-r (r/cursor working-copy [:outline_width])
        n-width-r (r/cursor working-copy [:note_width])]
    (fn [aps]
      [:div {:class "modal closed"
             :id    (id-basis->id (:id-basis settings))
             :role  "dialog"}
       [:header {:class "modal-header"}
        [:section {:class "modal-header-left"} (:header settings)]
        [:section {:class "modal-header-right"}
         [:input {:type     "button"
                  :class    "tree-demo--button"
                  :id       (str "close- " (:id-basis settings) "-button")
                  :value    "Close"
                  :title    (:cancel-button-popup-text settings)
                  :on-click #((:toggle-fn settings) settings)}]]]

       [:form {:class "modal-guts"}
        [:field-set {:class "prefs--container"}

         ;; The radio buttons to selection whether or not to load the last
         ;; document viewed the next time the program is loaded.
         [:div {:class "prefs--row"}
          [:label {:class "prefs--label"} "Load Last Document:"]
          ;[:input {:type      "text"
          ;         :class     "prefs--text-editor"
          ;         :value     (str (get-in @aps [:preferences :load-last-file]))
          ;         :on-change #(println "value: ")}]

          ;<label>Radio buttons</label>
          ;<input type = "radio"
          ;name = "radSize"
          ;id = "sizeSmall"
          ;value = "small"
          ;checked = "checked" />
          ;<label for = "sizeSmall">small</label>
          ;<input type = "radio"
          ;name = "radSize"
          ;id = "sizeMed"
          ;value = "medium" />
          ;; See https://stackoverflow.com/questions/8838648/onchange-event-handler-for-radio-button-input-type-radio-doesnt-work-as-one
          [:div {:class "prefs--radio-group"}
           [:input {:type      "radio"
                    :name      "load-last-file"
                    :id        "do-load-last-file"
                    :checked   (if @load-file-r
                                 "checked"
                                 nil)
                    :on-change #(do (println "YES LOAD changed")
                                    (if @load-file-r
                                      ; Changing from don't want it to want it.
                                      (do (println "want it now)"))
                                      (do (println "don't want it"))))
                    :value     "Yes"}]
           (quarter-rem-spacer)
           [:label {:for "do-load-last-file"} "Yes"]
           (one-rem-spacer)

           [:input {:type      "radio"
                    :name      "load-last-file"
                    :id        "do-not-load-last-file"
                    :checked   (if @load-file-r             ;(get-in @aps [:preferences :load-last-file])
                                 nil
                                 "checked")
                    :on-change #(do (println "DONT LOAD changed")
                                    (if @load-file-r
                                      ; Changing from not wanting it to wanting it.
                                      (do (println "Want it"))
                                      (do (println "Don' want it"))))
                    :value     "No"}]
           (quarter-rem-spacer)
           [:label {:for "do-not-load-last-file"} "No"]]
          (half-rem-spacer)
          [:label {:class "prefs--help-char"
                   :title (str "Whether to load the last document viewed the "
                               "next time the program is started.")} "?"]]

         ;; Autosave interval.
         [:div {:class "prefs--row"}
          [:label {:class "prefs--label"} "Autosave Interval:"]
          [:input {:type      "number"
                   :class     "prefs--text-editor"
                   :step      1
                   :value     @interval-r
                   :on-change #(reset! interval-r (js/parseInt (du/event->target-value %)))}]
          (half-rem-spacer)
          [:label {:class "prefs--help-char"
                   :title (str "The number of idle seconds to wait for before "
                               "saving the document. A value of 0 means no "
                               "automatic saves will be done.")} "?"]]
         [:p {:class "prefs--help-text"
              :style {:color "red"}}
          "The \"Autosave\" function is not ready yet."]

         ;; Column width for outliner.
         [:div {:class "prefs--row"}
          [:label {:class "prefs--label"} "Outliner Column Width:"]
          [:input {:type      "number"
                   :class     "prefs--text-editor"
                   :step      1
                   :value     @o-width-r
                   :on-change #(reset! o-width-r (js/parseInt (du/event->target-value %)))}]
          (half-rem-spacer)
          [:label {:class "prefs--help-char"
                   :title (str "The width of the outliner column, in pixels. "
                               "This can also be set by dragging the right border "
                               "of the column with the mouse.")} "?"]]

         ;; Column width for notes.
         [:div {:class "prefs--row"}
          [:label {:class "prefs--label"} "Note Column Width:"]
          [:input {:type      "number"
                   :class     "prefs--text-editor"
                   :step      1
                   :value     @n-width-r
                   :on-change #(reset! n-width-r (js/parseInt (du/event->target-value %)))}]
          (half-rem-spacer)
          [:label {:class "prefs--help-char"
                   :title (str "The width of the note column, in pixels. "
                               "This can also be set by dragging the right border "
                               "of the column with the mouse.")} "?"]]]]

       ;; The footer with the cancel/confirm buttons.
       [:div {:class "modal-footer"}
        [:section {:class "tree-demo--button-area"}
         [:input {:type     "button"
                  :class    "tree-demo--button button-bar-item"
                  :value    (:cancel-button-text settings)
                  :title    (:cancel-button-popup-text settings)
                  :on-click #((:cancel-action-fn settings))}]
         [:input {:type     "button"
                  :class    "tree-demo--button button-bar-item"
                  :id       (id-basis->cancel-id (:id-basis settings))
                  :value    (:confirm-button-text settings)
                  :title    (:confirm-button-popup-text settings)
                  :on-click #(save-confirmed-changes! aps original-values working-copy)
                  :disabled (= original-values @working-copy)}]]]])))

(defn test-confirm-cancel-dialog
  [aps]
  [preferences-dialog-component
   ;confirm-cancel-dialog-template
   aps confirm-cancel-test-params])
