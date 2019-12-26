;;;;
;;;; This namespace contains some functions that were used in the
;;;; demonstration program for the outliner. They are here for reference
;;;; use when similar functions are added to the outliner program.
;;;;

(ns clown.client.buttons
  (:require [cljs.tools.reader.edn :as edn]
            [clojure.set :as cs]
            [clojure.string :as str]
            [clown.client.util.dialogs :as dlg]
            [clown.client.util.mru :refer [push-on-mru persist-new-mru]]
            [clown.client.util.undo-redo :as ur]
            [reagent.core :as r]
            [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                                trace debug info warn error]]))

;;;-----------------------------------------------------------------------------
;;; Buttons for the demo.

(defn add-reset-button
  "Return a function that will create a button that, when clicked, will undo
  all changes made to the tree since the program was started."
  [app-state-ratom]
  (let [button-id "reset-button"
        reset-fn (fn [_]
                   (let [um (:undo-redo-manager @app-state-ratom)]
                     (while (ur/can-undo? um)
                       (ur/undo! um))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :disabled "true"
        :title    "Reset the tree to its original state"
        :value    "Reset"
        :on-click #(reset-fn %)}])))

(defn add-new-button
  "Return a function that will produce a button that, when clicked,
  will delete the current contents of the control and replace it with a
  fresh, empty version."
  [app-state-ratom]
  (let [button-id "new-button"
        my-cursor (r/cursor app-state-ratom [:tree])
        ;empty-tree [(new-topic)]
        ;empty-tree-id (tree-id-parts->tree-id-string (conj (root-parts) "editor"))
        new-fn (fn [_]
                 ;         (reset! my-cursor empty-tree)
                 (r/after-render
                   (fn []
                     ;             (resize-textarea empty-tree-id)
                     ;             (highlight-and-scroll-editor-for-id
                     ;               empty-tree-id 0
                     (count (:topic (first @my-cursor))))))
        ;)
        ]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type  "button"
        :id       button-id
        :disabled "true"
        :title "Remove all contents from the tree control and start anew"
        :value "New"
        ;:on-click #(new-fn %)
        }])))

(defn file-ext
  "Return the file extension of the file name, lower-case, no dot."
  [file-name]
  (str/lower-case (last (str/split file-name #"\."))))

(defn vet-and-load-edn-outline
  "Assure that the data represents correct EDN and what we expect in an
  outline. When determined to be Ok, load it to be displayed."
  [aps file-name file-data]
  (when-let [outline (edn/read-string file-data)]
    (if-not (get-in outline [:outline :title])
      (dlg/toggle-bad-outline-modal)
      (do
        (push-on-mru aps file-name)
        (swap! aps assoc :current-outline (:outline outline))))))

(defn vet-and-load-outline
  [aps file-name file-data]
  (debugf "vet-and-load-outline: file-name: %s" file-name)
  (let [ext (file-ext file-name)]
    (cond
      (= "edn" ext)
      (vet-and-load-edn-outline aps file-name file-data)

      (= "opml" ext)
      (println "got an OPML file")

      :default
      (println "got an unusable file"))))

;; See:
;; https://stackoverflow.com/questions/55931282/how-to-preview-an-image-and-filename-for-a-file-upload-in-clojure
(defn handle-file-open-selection
  "A selection has been made in the File Open dialog. Grab the file and
  and load it into the program."
  [aps evt]
  (debug "handle-file-open-selection")
  (let [js-file (aget (.-files (.-target evt)) 0)
        file-name (.-name js-file)
        js-file-reader (js/FileReader.)
        source-data-atom (atom nil)]
    (set! (.-onload js-file-reader)
          #(do (reset! source-data-atom (-> % .-target .-result))
               (vet-and-load-outline aps (.-name js-file) @source-data-atom)))
    (set! (.-onerror js-file-reader)
          #(dlg/toggle-file-read-error-modal))
    (.readAsText js-file-reader js-file)))

(defn add-open-button
  "Return a function that will display a 'File Open' dialog. If a response
  is received, open that file and load any outline contained."
  [app-state-ratom]
  (let [button-id "open-button"
        file-open-id "file-open-id"
        open-fn #(.click (.getElementById js/document file-open-id))]
    (fn [app-state-ratom]
      [:div
       [:input {:type      "file" :id file-open-id
                :accept    ".edn, .opml"
                :multiple  nil
                :style     {:display "none"}
                ;;https://www.richardkotze.com/top-tips/preview-selected-images-for-uploading
                ;; also talks about dragging to open file.
                :on-change #(handle-file-open-selection app-state-ratom %)}]
       [:input.tree-demo--button
        {:type     "button"
         :id       button-id
         :title    "Open a new outline"
         :value    "Open"
         :on-click open-fn}]])))

(defn add-save-button
  "Return a function that will produce a button that, when clicked,
  will save the current state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "save-button"
        save-fn (fn [_] (.setItem (.-localStorage js/window) "tree"
                                  (pr-str (:tree @app-state-ratom))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :disabled "true"
        :title    "Save the current state of the tree"
        :value    "Save"
        :on-click #(save-fn %)}])))

(defn add-read-button
  "Return a function that will produce a button that, when clicked,
  will read the saved state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "read-button"
        read-fn (fn [_]
                  (when-let [data (.getItem (.-localStorage js/window) "tree")]
                    (let [edn (edn/read-string data)]
                      (swap! app-state-ratom assoc :tree edn))))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :disabled "true"
        :title    "Read the saved tree from storage"
        :value    "Read"
        :on-click #(read-fn %)}])))

(defn add-buttons
  "Adds buttons to the button bar."
  [app-state-ratom]
  (fn [app-state-ratom]
    [:div.tree-demo--button-area
     [add-reset-button app-state-ratom]
     [add-new-button app-state-ratom]
     [add-open-button app-state-ratom]
     [add-save-button app-state-ratom]
     [add-read-button app-state-ratom]]))

