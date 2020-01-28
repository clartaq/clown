(ns clown.client.util.empty-outline
  (:require [cljs-time.core :as dtc]
            [cljs-time.format :as dtf]
            [clown.client.tree-ids :as ti]))

(defn empty-outline-file-name
  []
  "Unsaved_Outline_File.edn")

(def clown-formatter (dtf/formatter "d MMM yyyy, h:mm:ssa"))

(defn formatted-time-now
  "Return the current local time formatted as \"26 Dec 2019, 3:21:45pm\"."
  []
  (dtf/unparse clown-formatter (dtc/time-now)))

(defn build-empty-outline
  [aps]
  (let [formatted-now (formatted-time-now)
        author (get-in @aps [:preferences :user])]
    {:version          "0.0.1"
     :author           author
     :title            "Unsaved Outline"
     :created          formatted-now
     :modified         formatted-now
     :focused-headline (ti/top-tree-id)
     :dirty?           nil
     :tree             [{:topic "Your outline here"}]}))