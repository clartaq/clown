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

(defn id-basis->cancel-id
  [basis]
  (str basis "-cancel-button-id"))


