;;;;
;;;; This namespace contains simple, single button informational dialogs.
;;;;

(ns clown.client.dialogs.ok-dialogs
  (:require [clown.client.util.dom-utils :as du]
            [clown.client.dialogs.util :as dlgu]))

(defn- toggle-modal
  "Toggle the display state of the modal dialog with the given id."
  [ele-id]
  (let [close-button (du/get-element-by-id ele-id)
        overlay (du/get-element-by-id "modal-overlay")]
    (when (and close-button overlay)
      (du/toggle-classlist close-button "closed")
      (du/toggle-classlist overlay "closed"))))

(defn one-button-dialog-template
  "Return a piece of hiccup for a one button dialog with the given features."
  [settings]
  (let [toggle-fn (:toggle-fn settings)]
    [:div {:class "modal closed"
           :id    (dlgu/id-basis->id (:id-basis settings))
           :role  "dialog"}
     [:header {:class "modal-header"}
      [:section {:class "modal-header-left"} (:header settings)]
      [:section {:class "modal-header-right"}
       [:input {:type     "button"
                :class    "tree-demo--button"
                :id       (str "close- " (:id-basis settings) "-button")
                :value    "Close"
                :title    (:button-help settings)
                :on-click toggle-fn}]]]
     [:section {:class "modal-guts"} (dlgu/enumerate-message-paragraphs (:message-text settings))]
     [:div {:class "modal-footer"}
      [:section {:class "tree-demo--button-area"}
       [:input {:type     "button"
                :class    "tree-demo--button button-bar-item"
                :value    (:button-text settings)
                :title    (:button-help settings)
                :on-click toggle-fn}]]]]))

;;;-----------------------------------------------------------------------------
;;; The "About to Update the MRU" dialog.

(def about-to-update-mru-basis "about-to-update-mru")

(defn toggle-about-to-update-mru-modal
  []
  (toggle-modal (dlgu/id-basis->id about-to-update-mru-basis)))

(defn layout-about-to-update-mru-modal
  []
  [one-button-dialog-template {:id-basis     about-to-update-mru-basis
                               :header       "About to Update"
                               :message-text ["About the update the MRU list in the user preferences."
                                              "Return to the editor and change the title before saving."]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-about-to-update-mru-modal}])

;;;-----------------------------------------------------------------------------
;;; The "Bad EDN" dialog.

(def bad-edn-basis "bad-edn")

(defn toggle-bad-edn-modal
  []
  (toggle-modal (dlgu/id-basis->id bad-edn-basis)))

(defn layout-bad-edn-modal
  []
  [one-button-dialog-template {:id-basis     bad-edn-basis
                               :header       "Sorry! Bad EDN."
                               :message-text ["The file loaded does not appear to contain valid EDN format data."
                                              "Try opening another file with the EDN extension."]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-bad-edn-modal}])

;;;-----------------------------------------------------------------------------
;;; The "Bad Outline" dialog.

(def bad-outline-basis "bad-outline")

(defn toggle-bad-outline-modal
  []
  (toggle-modal (dlgu/id-basis->id bad-outline-basis)))

(defn layout-bad-outline-modal
  []
  [one-button-dialog-template {:id-basis     bad-outline-basis
                               :header       "Sorry! Bad Outline."
                               :message-text ["The file does not seem to contain a properly formatted outline."
                                              "Try opening another outline."]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-bad-outline-modal}])

;;;-----------------------------------------------------------------------------
;;; The "File Read Error" dialog.

(def file-read-error-basis "file-read-error")

(defn toggle-file-read-error-modal
  []
  (toggle-modal (dlgu/id-basis->id file-read-error-basis)))

(defn layout-file-read-error-dlg
  []
  [one-button-dialog-template {:id-basis     file-read-error-basis
                               :header       "Sorry! File Problem."
                               :message-text ["Some type of error occurred when trying to read the file."
                                              "Try loading another file or check if the requested file has been moved or deleted."]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-file-read-error-modal}])

;;;-----------------------------------------------------------------------------
;;; The "File Does Not Exist" dialog.

(def file-does-not-exist-basis "file-does-not-exist")

(defn toggle-file-does-not-exist-modal
  [file-name]
  (prn "file-name: " file-name)
  (toggle-modal (dlgu/id-basis->id file-does-not-exist-basis)))

(defn layout-file-does-not-exist-dlg
  []
  [one-button-dialog-template {:id-basis     file-does-not-exist-basis
                               :header       "Sorry! File Problem."
                               :message-text ["The requested file does not exist."
                                              "Try loading another file or check if the requested file has been moved or deleted."]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-file-does-not-exist-modal}])

;;;---------------------------------------------------------------------------------------------------------------------
;;; The "About" dialog.

(def program-info {:name        "clown"
                   :description "A Clojure/Script outliner with notes."
                   :version     "0.1.1-SNAPSHOT"
                   :author      "David D. Clark"
                   :license     {:name  "EPL v1.0"
                                 :url   "https://www.eclipse.org/legal/epl-v10.html"
                                 :local "See `LICENSE.txt."}
                   :url         "https://github.com/clartaq/clown"
                   :copyright   "Copyright © 2019-2020, David D. Clark"})

(def about-basis "about")

(defn toggle-about-modal
  []
  (toggle-modal (dlgu/id-basis->id about-basis)))

(defn layout-about-dlg
  []
  [one-button-dialog-template {:id-basis     about-basis
                               :header       (str "About " (:name program-info))
                               :message-text [(str "Description: " (:description program-info))
                                              (str "Version: " (:version program-info))
                                              (str "Author: " (:author program-info))
                                              (str "License: " (get-in program-info [:license :name]))
                                              (str "Repository: " (:url program-info))
                                              (:copyright program-info)]
                               :button-text  "Ok. Return to the Outliner."
                               :button-help  "Close this dialog and return to the outliner."
                               :toggle-fn    toggle-about-modal}])
