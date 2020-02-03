;;;
;;; This namespace contains the code for a "not-saved" dialog to be invoked
;;; when the user is about to overwrite an open document that has changes
;;; from the version originally loaded into the program. For example, when
;;; the open document has changes and the user tries to create a new document
;;; or when they attempt to open an existing document from disk before saving
;;; the changes made to the existing open document

(ns clown.client.dialogs.not-saved-dialog
  (:require [clown.client.dialogs.util :as dlgu]
            [clown.client.util.dom-utils :as du]))

(defn de-activate-glass-pane
  "De-activate (make invisible) the glass pane."
  []
  (du/set-style-property-value "modal-overlay" "display" "none"))

(defn activate-glass-pane
  "Activate (make visible) the glass pane, preventing interaction with any
  elements below it in the z-stack."
  []
  (du/set-style-property-value "modal-overlay" "display" "block"))

;; Data from mostly failed attempt to create a parameterized confirm/cancel
;; type dialog. Parts are still used here.
(def confirm-cancel-dialog-params
  {:id-basis                  "not-saved-basis"
   :header                    "Unsaved Changes!"
   :message-text              [(str "There are unsaved changes in the document "
                                    "you were working on.")
                               (str "If you really want to abandon those "
                                    "changes and proceed, click the \"Abandon\" "
                                    "button below.")
                               (str "If you want to return to the program for "
                                    "a chance to save those changes, click the "
                                    "\"Cancel\" button below.")]
   :confirm-button-text       "Abandon"
   :cancel-button-text        "Cancel"
   :confirm-button-popup-text "Abandon all changes and overwrite the document."
   :cancel-button-popup-text  "Cancel and return to the outliner."})

(defn not-saved-dialog
  "Layout the not-saved dialog and handlers."
  [aps settings]
  (fn [aps settings]
    (when (:show-not-saved-dialog @aps)
      (activate-glass-pane)
      (let [modal-id (dlgu/id-basis->id (:id-basis settings))
            ;; The overwriting-fn takes one argument, the program state ratom.
            overwriting-fn (:overwriting-fn @aps)
            close-dlg (fn []
                        (de-activate-glass-pane)
                        (swap! aps dissoc :show-not-saved-dialog))]

        ;; We never actually dissoc the :overwriting-fn since that would
        ;; cause a re-render before the overwriting-fn gets a chance to
        ;; execute.

        [:div {:class "modal"
               :id    modal-id
               :role  "dialog"}
         [:header {:class "modal-header"}
          [:section {:class "modal-header-left"} (:header settings)]
          [:section {:class "modal-header-right"}
           [:input {:type     "button"
                    :class    "tree-demo--button"
                    :id       (str "close- " (:id-basis settings) "-button")
                    :value    "Close"
                    :title    (:cancel-button-popup-text settings)
                    :on-click close-dlg}]]]

         ;; The message.
         [:form {:class "modal-guts"}
          [:field-set {:class "prefs--container"}
           [:section (dlgu/enumerate-message-paragraphs (:message-text settings))]]]

         ;; The footer with the cancel/confirm buttons.
         [:div {:class "modal-footer"}
          [:section {:class "tree-demo--button-area"}
           [:input {:type     "button"
                    :class    "tree-demo--button button-bar-item"
                    :id       "pref-dialog-cancel-button-id"
                    :value    (:cancel-button-text settings)
                    :title    (:cancel-button-popup-text settings)
                    :on-click close-dlg}]
           [:input {:type     "button"
                    :class    "tree-demo--button button-bar-item"
                    :id       (dlgu/id-basis->confirm-id (:id-basis settings))
                    :value    (:confirm-button-text settings)
                    :title    (:confirm-button-popup-text settings)
                    :on-click #(do
                                 (overwriting-fn aps)
                                 (close-dlg))}]]]]))))

(defn layout-not-saved-dialog
  "Layout a parameterized preferences dialog."
  [aps]
  [not-saved-dialog aps confirm-cancel-dialog-params])
