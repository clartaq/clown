(ns clown.client.util.empty-outline
  (:require [clown.client.tree-ids :as ti]
            [clown.client.util.clown-time :as ct]))

(defn empty-outline-file-name
  []
  "Unsaved_Outline_File.edn")

(defn build-empty-outline
  [aps]
  (let [formatted-now (ct/formatted-time-now)
        author (get-in @aps [:preferences :user])]
    {:version          "0.0.1"
     :author           author
     :title            "Unsaved Outline"
     :created          formatted-now
     :modified         formatted-now
     :focused-headline (ti/top-tree-id)
     :dirty?           nil
     :tree             [{:topic "Your outline here"}]}))