(ns clown.client.util.dialogs
  (:require [clown.client.util.dom-utils :as du]))

(defn- toggle-modal
  "Toggle the display state of the modal dialog with the given id."
  [ele-id]
  (let [close-button (du/get-element-by-id ele-id)
        overlay (du/get-element-by-id "modal-overlay")]
    (when (and close-button overlay)
      (.toggle (.-classList close-button) "closed")
      (.toggle (.-classList overlay) "closed"))))

(defn toggle-about-to-update-mru-modal
  []
  (toggle-modal "about-to-update-mru-modal"))

(defn layout-about-to-update-mru-modal
  []
  (let [msg [:div
             [:p {:class "dialog-message"}
              "About the update the MRU list in the user preferences."]
             [:p {:class "dialog-message"}
              "Return to the editor and change the title before saving."]]]
    [:div {:class "modal closed" :id "about-to-update-mru-modal" :role "dialog"}
     [:header {:class "modal-header"}
      [:section {:class "modal-header-left"} "About to Update"]
      [:section {:class "modal-header-right"}
       [:input {:type     "button"
                :class    "tree-demo--button"
                :id       "close-about-to-update-button"
                :value    "Close"
                :title    "Close this dialog and return to the outliner."
                :on-click #(toggle-about-to-update-mru-modal)}]]]
     [:section {:class "modal-guts"} msg]
     [:div {:class "modal-footer"}
      [:section {:class "tree-demo--button-area"}
       [:input {:type     "button"
                :class    "tree-demo--button button-bar-item"
                :value    "Ok. Return to the Outliner."
                :title    "Close this dialog and return to the outliner"
                :on-click #(toggle-about-to-update-mru-modal)}]]]]))

(defn toggle-bad-edn-modal
  []
  (toggle-modal "bad-edn-modal"))

(defn layout-bad-edn-modal
  []
  (let [msg [:div
             [:p {:class "dialog-message"}
              "The file loaded does not appear to contain valid EDN format data."]
             [:p {:class "dialog-message"}
              "Try opening another file with the EDN extension."]]]
    [:div {:class "modal closed" :id "bad-edn-modal" :role "dialog"}
     [:header {:class "modal-header"}
      [:section {:class "modal-header-left"} "Sorry! Bad EDN."]
      [:section {:class "modal-header-right"}
       [:input {:type     "button"
                :class    "tree-demo--button"
                :id       "close-bad-edn-button"
                :value    "Close"
                :title    "Close this dialog and return to the outliner."
                :on-click #(toggle-bad-edn-modal)}]]]
     [:section {:class "modal-guts"} msg]
     [:div {:class "modal-footer"}
      [:section {:class "tree-demo--button-area"}
       [:input {:type     "button"
                :class    "tree-demo--button button-bar-item"
                :value    "Ok. Return to the Outliner."
                :title    "Close this dialog and return to the outliner"
                :on-click #(toggle-bad-edn-modal)}]]]]))

(defn toggle-bad-outline-modal
  []
  (toggle-modal "bad-outline-modal"))

(defn layout-bad-outline-modal
  []
  (let [msg [:div
             [:p {:class "dialog-message"}
              "The file does not seem to contain a properly formatted outline."]
             [:p {:class "dialog-message"}
              "Try opening another outline."]]]
    [:div {:class "modal closed" :id "bad-outline-modal" :role "dialog"}
     [:header {:class "modal-header"}
      [:section {:class "modal-header-left"} "Sorry! Bad Outline."]
      [:section {:class "modal-header-right"}
       [:input {:type     "button"
                :class    "tree-demo--button"
                :id       "close-bad-outline-button"
                :value    "Close"
                :title    "Close this dialog and return to the outliner."
                :on-click #(toggle-bad-outline-modal)}]]]
     [:section {:class "modal-guts"} msg]
     [:div {:class "modal-footer"}
      [:section {:class "tree-demo--button-area"}
       [:input {:type     "button"
                :class    "tree-demo--button button-bar-item"
                :value    "Ok. Return to the Outliner."
                :title    "Close this dialog and return to the outliner"
                :on-click #(toggle-bad-outline-modal)}]]]]))

(defn toggle-file-read-error-modal
  []
  (toggle-modal "file-read-error-modal"))

(defn layout-file-read-error-dlg
  []
  (let [msg [:div
             [:p {:class "dialog-message"}
              "Some type of error occurred when trying to read the file."]
             [:p {:class "dialog-message"}
              "Try loading another file or check if the requested file has been moved or deleted."]]]
    [:div {:class "modal closed" :id "file-read-error-modal" :role "dialog"}
     [:header {:class "modal-header"}
      [:section {:class "modal-header-left"} "Sorry! File Problem."]
      [:section {:class "modal-header-right"}
       [:input {:type     "button"
                :class    "tree-demo--button"
                :id       "close-file-error-button"
                :value    "Close"
                :title    "Close this dialog and return to the outliner."
                :on-click #(toggle-file-read-error-modal)}]]]
     [:section {:class "modal-guts"} msg]
     [:div {:class "modal-footer"}
      [:section {:class "tree-demo--button-area"}
       [:input {:type     "button"
                :class    "tree-demo--button button-bar-item"
                :value    "Ok. Return to the Outliner."
                :title    "Close this dialog and return to the outliner"
                :on-click #(toggle-file-read-error-modal)}]]]]))
