(ns clown.client.util.clown-time
  (:require [cljs-time.core :as dtc]
            [cljs-time.format :as dtf]))

(def clown-formatter (dtf/formatter "d MMM yyyy, h:mm:ssa"))

(defn formatted-time-now
  "Return the current local time formatted as \"26 Dec 2019, 3:21:45pm\"."
  []
  (dtf/unparse clown-formatter (dtc/time-now)))


