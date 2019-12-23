(ns clown.server.util.date-time
  (:import (java.time LocalTime)
           (java.time.format DateTimeFormatter)))

(def time-formatter (DateTimeFormatter/ofPattern "h:mm:ss a"))

(defn formatted-now
  "Return a string with the current local time."
  []
  (.format (LocalTime/now) time-formatter))
