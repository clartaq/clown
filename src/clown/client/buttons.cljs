(ns clown.client.buttons
  (:require [cljs.tools.reader.edn :as edn]
            [clojure.string :as str]
            [clown.client.commands :as cmd]
            [clown.client.dialogs.ok-dialogs :as dlg]
            [clown.client.util.dom-utils :as du]
            [clown.client.util.marker :as mrk]
            [clown.client.util.mru :refer [push-on-mru! persist-new-mru]]
            [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                                trace debug info warn error]]))

(defn add-new-button
  "Return a function that will produce a button that, when clicked,
  will delete the current contents of the control and replace it with a
  fresh, empty version."
  [app-state-ratom]
  (let [button-id "new-button"]
    (fn [app-state-ratom]
      [:input.tree-demo--button
       {:type     "button"
        :id       button-id
        :title    "Erase the current outline and start with a new one."
        :value    "New"
        :on-click #(if (mrk/dirty? app-state-ratom)
                     (do
                       (swap! app-state-ratom assoc :overwriting-fn cmd/new-outline)
                       (swap! app-state-ratom assoc :show-not-saved-dialog true))
                     (cmd/new-outline app-state-ratom))}])))

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
        (mrk/mark-as-clean! aps)
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
  (let [js-file (aget (du/event->target-files evt) 0)
        js-file-reader (js/FileReader.)
        source-data-atom (atom nil)]
    (set! (.-onload js-file-reader)
          (fn [evt]
            (reset! source-data-atom (du/event->target-result evt))
            (vet-and-load-outline aps (.-name js-file) @source-data-atom)))
    (set! (.-onerror js-file-reader)
          #(dlg/toggle-file-read-error-modal))
    (du/read-file-as-text js-file-reader js-file)
    ;; Solution provided by answer to question on StackOverflow:
    ;; https://stackoverflow.com/questions/59592261/problem-opening-files-with-the-filereader-api/60057413#60057413
    (set! (.-value (du/get-element-by-id "file-open-id")) "")))

(defn select-and-load
  [aps]
  (let [click-fn (fn [_] (du/click-element-with-id "file-open-id"))]
    (if (mrk/dirty? aps)
      (do
        (swap! aps assoc :overwriting-fn click-fn)
        (swap! aps assoc :show-not-saved-dialog true))
      (click-fn nil))))

(defn add-open-button
  "Return a function that will display a 'File Open' dialog. If a response
  is received, open that file and load any outline contained."
  [app-state-ratom]
  (let [button-id "open-button"
        file-open-id "file-open-id"]
    (fn [app-state-ratom]
      [:div
       [:input {:type      "file"
                :id        file-open-id
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
         :on-click #(select-and-load app-state-ratom)}]])))

(defn add-save-button
  "Return a function that will produce a button that, when clicked,
  will save the current state of the tree in local storage."
  [app-state-ratom]
  (let [button-id "save-button"
        save-fn (fn [evt]
                  (du/prevent-default evt)
                  (du/stop-propagation evt)
                  (cmd/save-outline-as-edn! app-state-ratom)
                  (mrk/mark-as-clean! app-state-ratom))]
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

