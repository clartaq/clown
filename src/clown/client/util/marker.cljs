;;;;
;;;; Some utility functions to manipulate the "dirty" state of an document.
;;;;

(ns clown.client.util.marker)

(defn dirty?
  "Return whether or not the document in the program state is marked as dirty."
  [aps]
  (:dirty? @aps))

(defn mark-as-dirty!
  "Return the updated program state where the document is marked as dirty."
  [aps]
  (when (map? @aps)
    (swap! aps assoc :dirty? true)))

(defn mark-as-clean!
  "Return the updated program state where the document is marked as clean."
  [aps]
  (when (map? @aps)
    (swap! aps assoc :dirty? nil)))