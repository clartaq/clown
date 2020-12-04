;;;
;;; Simple utility functions used by the dialogs.
;;;

(ns clown.client.dialogs.util)

;;;-----------------------------------------------------------------------------
;;; Fixed-width horizontal spacers.

(defn one-rem-spacer
  []
  [:label {:class "prefs--one-rem-spacer"} " "])

(defn half-rem-spacer
  []
  [:label {:class "prefs--half-rem-spacer"} " "])

(defn quarter-rem-spacer
  []
  [:label {:class "prefs--quarter-rem-spacer"} " "])

;;;-----------------------------------------------------------------------------
;;; Assure consistent id generation.

(defn id-basis->id
  [basis]
  (str basis "-modal"))

(defn id-basis->close-id
  [basis]
  (str basis "-close-button-id"))

(defn id-basis->confirm-id
  [basis]
  (str basis "-confirm-button-id"))

(defn id-basis->cancel-id
  [basis]
  (str basis "-cancel-button-id"))

(defn enumerate-message-paragraphs
  "Return a div containing each element in the array of text as paragraphs."
  [ta]
  (into [:div] (mapv #(into [:p.dialog-message %]) ta)))




