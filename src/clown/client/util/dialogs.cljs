(ns clown.client.util.dialogs
  (:require [clown.client.util.dom-utils :as du]))

(defn- toggle-modal
  "Toggle the display state of the modal dialog with the given id."
  [ele-id]
  (let [close-button (du/get-element-by-id ele-id)
        overlay (du/get-element-by-id "modal-overlay")]
    (when (and close-button overlay)
      (du/toggle-classlist close-button "closed")
      (du/toggle-classlist overlay "closed"))))

(defn enumerate-message-paragraphs
  "Return a div containing each element in the array of text as paragraphs."
  [ta]
  (into [:div] (mapv #(into [:p.dialog-message %]) ta)))

(defn one-button-dialog-template
  "Return a piece of hiccup for a one button dialog with the given features."
  [settings]
  [:div {:class "modal closed" :id (str (:id-basis settings) "-modal")
         :role  "dialog"}
   [:header {:class "modal-header"}
    [:section {:class "modal-header-left"} (:header settings)]
    [:section {:class "modal-header-right"}
     [:input {:type     "button"
              :class    "tree-demo--button"
              :id       (str "close- " (:id-basis settings) "-button")
              :value    "Close"
              :title    (:button-help settings)
              :on-click #((:toggle-fn settings))}]]]
   [:section {:class "modal-guts"} (enumerate-message-paragraphs (:msg-text settings))]
   [:div {:class "modal-footer"}
    [:section {:class "tree-demo--button-area"}
     [:input {:type     "button"
              :class    "tree-demo--button button-bar-item"
              :value    (:button-text settings)
              :title    (:button-help settings)
              :on-click #((:toggle-fn settings))}]]]])

(defn toggle-about-to-update-mru-modal
  []
  (toggle-modal "about-to-update-mru-modal"))

(defn layout-about-to-update-mru-modal
  []
  [one-button-dialog-template {:id-basis    "about-to-update-mru"
                               :header      "About to Update"
                               :msg-text    ["About the update the MRU list in the user preferences."
                                             "Return to the editor and change the title before saving."]
                               :button-text "Ok. Return to the Outliner."
                               :button-help "Close this dialog and return to the outliner."
                               :toggle-fn   toggle-about-to-update-mru-modal}])

(defn toggle-bad-edn-modal
  []
  (toggle-modal "bad-edn-modal"))

(defn layout-bad-edn-modal
  []
  [one-button-dialog-template {:id-basis    "bad-edn"
                               :header      "Sorry! Bad EDN."
                               :msg-text    ["The file loaded does not appear to contain valid EDN format data."
                                             "Try opening another file with the EDN extension."]
                               :button-text "Ok. Return to the Outliner."
                               :button-help "Close this dialog and return to the outliner."
                               :toggle-fn   toggle-bad-edn-modal}])

(defn toggle-bad-outline-modal
  []
  (toggle-modal "bad-outline-modal"))

(defn layout-bad-outline-modal
  []
  [one-button-dialog-template {:id-basis    "bad-outline"
                               :header      "Sorry! Bad Outline."
                               :msg-text    ["The file does not seem to contain a properly formatted outline."
                                             "Try opening another outline."]
                               :button-text "Ok. Return to the Outliner."
                               :button-help "Close this dialog and return to the outliner."
                               :toggle-fn   toggle-bad-outline-modal}])

(defn toggle-file-read-error-modal
  []
  (toggle-modal "file-read-error-modal"))

(defn layout-file-read-error-dlg
  []
  [one-button-dialog-template {:id-basis    "file-read-error"
                               :header      "Sorry! File Problem."
                               :msg-text    ["Some type of error occurred when trying to read the file."
                                             "Try loading another file or check if the requested file has been moved or deleted."]
                               :button-text "Ok. Return to the Outliner."
                               :button-help "Close this dialog and return to the outliner."
                               :toggle-fn   toggle-file-read-error-modal}])
