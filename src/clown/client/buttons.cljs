(ns clown.client.buttons
  (:require [cljs.tools.reader.edn :as edn]
            [clojure.string :as str]
            [clown.client.commands :as cmd]
            [clown.client.util.ok-dialogs :as dlg]
            [clown.client.util.dom-utils :as du]
            [clown.client.util.empty-outline :as eo]
            [clown.client.util.mru :refer [push-on-mru! persist-new-mru]]
            [clown.client.util.undo-redo :as ur]
            [reagent.core :as r]
            [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                                trace debug info warn error]]))

(defn add-new-button
  "Return a function that will produce a button that, when clicked,
  will delete the current contents of the control and replace it with a
  fresh, empty version."
  [app-state-ratom]
  (let [button-id "new-button"
        new-fn (fn [_]
                 ;; BUG HERE!!! This doesn't really seem to work as expected.
                 ;; It's like the new undo-manager never gets attached to the
                 ;; ratom. The stuff from the old ratom is still there. You can
                 ;; load files, make changes, etc, and then back up through all
                 ;; the file changes (without resetting outline title and
                 ;; file name) all the way back to when the app was loaded.
                 (swap! app-state-ratom assoc :current-outline
                        (eo/build-empty-outline app-state-ratom))
                 (push-on-mru! app-state-ratom (eo/empty-outline-file-name)))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Erase the current outline and start with a new one."
        :value    "New"
        :on-click #(new-fn %)}])))

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
        (push-on-mru! aps file-name)
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
        sim-click-fn #(.click (du/get-element-by-id file-open-id))]
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
         :on-click sim-click-fn}]])))

(defn add-save-button
  "Return a function that will produce a button that, when clicked,
  will save the current state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "save-button"
        save-fn (fn [evt] (cmd/save-outline-as-edn! {:evt evt :root-ratom app-state-ratom}))]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Save the current state of the tree"
        :value    "Save"
        :on-click #(save-fn %)}])))

(defn add-buttons
  "Adds buttons to the button bar."
  [app-state-ratom]
  (fn [app-state-ratom]
    [:div.tree-demo--button-area
     [add-new-button app-state-ratom]
     [add-open-button app-state-ratom]
     [add-save-button app-state-ratom]]))

