;;;;
;;;; Some utility functions to manipulate the "dirty" state of an document and
;;;; do a timed autosave on altered documents.
;;;;

(ns clown.client.util.marker
  (:require [clown.client.util.empty-outline :as eo]))


;;;-----------------------------------------------------------------------------
;;; Auto-save-related functions.
;;;

;; The delay-handle stores the handle to the autosave countdown timer.
(def ^{:private true} glbl-delay-handle (atom nil))

(defn- clear-autosave-delay! []
  "Clear the autosave countdown timer."
  (.clearTimeout js/window @glbl-delay-handle))

(defn- restart-autosave-delay!
  "Restart the autosave countdown timer."
  [the-save-fn delay-ms]
  (reset! glbl-delay-handle (.setTimeout js/window the-save-fn delay-ms)))

(defn- notify-autosave
  "Notify the autosave functionality that a change has occurred. When the
  autosave duration is greater than zero, restarts the countdown until the
  document is saved automatically. Will NOT perform autosave until the title
  of new docs has been changed from the default for new docs."
  [aps]
  (let [autosave-interval (get-in @aps [:preferences :autosave_interval])
        default-new-file-name (eo/empty-outline-file-name)
        doc-file-name (first (get-in @aps [:preferences :mru]))
        the-save-fn (:save-doc-fn @aps)
        delay (* 1000 autosave-interval)]
    (when (and (pos? delay)
               (not= default-new-file-name doc-file-name))
      (clear-autosave-delay!)
      (restart-autosave-delay! #(the-save-fn aps) delay))))

;;;-----------------------------------------------------------------------------
;;; Functions to mark/unmark modified documents.
;;;

(defn dirty?
  "Return whether or not the document in the program state is marked as dirty."
  [aps]
  (:dirty? @aps))

(defn mark-as-dirty!
  "Return the updated program state where the document is marked as dirty."
  [aps]
  (when (map? @aps)
    (notify-autosave aps)
    (swap! aps assoc :dirty? true)))

(defn mark-as-clean!
  "Return the updated program state where the document is marked as clean."
  [aps]
  (when (map? @aps)
    (swap! aps assoc :dirty? nil)))