;;;;
;;;; This namespace contains general purpose functions to manipulate the
;;;; tree of the outline.
;;;;

(ns clown.client.tree-manip
  (:require [clown.client.util.dom-utils :as du]
            [clown.client.tree-ids :as ti]
            [clown.client.util.vector-utils :refer [delete-at remove-first
                                                    remove-last remove-last-two
                                                    insert-at replace-at
                                                    append-element-to-vector]]
            [reagent.core :as r]
            [reagent.dom.server :refer [render-to-string render-to-static-markup]]
            [taoensso.timbre :refer [tracef debugf infof warnf errorf
                                     trace debug info warn error]]))

;;------------------------------------------------------------------------------
;; Just a few miscellaneous utility functions.

(defn convert-to-number-or-not
  "If every character in the string is a digit, convert the string to a
  number and return it. Otherwise, return the string unchanged."
  [s]
  (if (du/all-digits? s)
    (js/parseInt s)
    s))

;; From: https://stackoverflow.com/questions/5232350/clojure-semi-flattening-a-nested-sequence
(defn flatten-to-vectors
  "Flatten nested sequences of vectors to a flat sequence of those vectors."
  [s]
  (mapcat #(if (every? coll? %) (flatten-to-vectors %) (list %)) s))

(defn positions
  "Return a list of the index positions of elements in coll that satisfy pred."
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

;;------------------------------------------------------------------------------
;; Functions to query and manipulate the tree and subtrees.

(defn tree->nav-vector-sequence
  "Return a sequence of (possibly nested) navigation vectors for all the nodes
  in the tree that satisfy the predicate. The sequence is generated from an
  'in-order' traversal."
  [tree so-far pred]
  (letfn [(helper [my-tree my-id-so-far]
            (map-indexed (fn [idx ele]
                           (let [new-id (conj my-id-so-far idx)]
                             (if-not (pred ele)
                               new-id
                               (cons new-id (helper (:children ele) new-id)))))
                         my-tree))]
    (helper tree so-far)))

(defn all-nodes
  "Return a sequence of vectors of the numerical indices used to travel from
  the root to each node in the tree. Includes nodes that may not be visible
  at the moment."
  [tree so-far]
  (flatten-to-vectors
    (tree->nav-vector-sequence tree so-far :children)))

(defn count-nodes
  [tree]
  (count (all-nodes tree ["root"])))

(defn last-node-in-tree
  "Return the tree-id of the last node in the tree."
  [root-ratom]
  (ti/tree-id-parts->tree-id-string
    (conj (last (all-nodes @root-ratom ["root"])) "topic")))

(defn visible-children?
  "Return true if the topic is expanded and has children."
  [topic-map]
  (and (:children topic-map) (:expanded topic-map)))

(defn visible-nodes
  "Return a sequence of vectors of the numerical indices used to travel from
  the root to each visible node."
  [tree so-far]
  (flatten-to-vectors
    (tree->nav-vector-sequence tree so-far visible-children?)))

(defn last-visible-node-in-tree
  "Return the tree-id of the last visible node in the tree."
  [root-ratom]
  (ti/tree-id-parts->tree-id-string
    (conj (last (visible-nodes @root-ratom ["root"])) "topic")))

(defn top-visible-tree-id?
  "Return the same result as top-tree-id? since the top of the tree is
  always visible."
  [_ tree-id]
  (ti/top-tree-id? tree-id))

(defn bottom-visible-tree-id?
  "Return true if the node with the given id is the bottom visible node in
  the tree; false otherwise."
  [root-ratom tree-id]
  (= tree-id (ti/tree-id-parts->tree-id-string
               (conj (last (visible-nodes @root-ratom ["root"])) "topic"))))

(defn get-topic
  "Return the topic map at the requested id. Return nil if there is
  nothing at that location."
  [root-ratom topic-id]
  (get-in @root-ratom (ti/tree-id->tree-path-nav-vector topic-id)))

(defn children?
  "Return the entire vector of children if present, nil otherwise."
  [root-ratom topic-id]
  (:children (get-topic root-ratom topic-id)))

(defn count-children
  "Return the number of children of the topic."
  [root-ratom topic-id]
  (count (children? root-ratom topic-id)))

(defn where-to-append-next-child
  "Return the location (tree id) where the next sibling should be added to
  a parent. That position is one below the last child or the first child if
  the parent has no children."
  [root-ratom parent-id]
  (let [number-of-children (count-children root-ratom parent-id)
        first-child (ti/id-of-first-child parent-id)]
    (ti/set-leaf-index first-child number-of-children)))

(defn expanded?
  "Return true if the subtree is in the expanded state (implying that it
  has children). Returns nil if the subtree is not expanded."
  [root-ratom tree-id]
  (:expanded (get-topic root-ratom tree-id)))

(defn expand-node!
  "Assure that the node is expanded."
  [root-ratom tree-id]
  (let [nav-vector (ti/tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded true)))

(defn collapse-node!
  "Assure that the node is collapsed."
  [root-ratom tree-id]
  (let [nav-vector (ti/tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (swap! my-cursor assoc :expanded nil)))

(defn toggle-node-expansion!
  "Toggle the 'expanded' setting for the node. When the branch has no
  :expanded key, does nothing."
  [root-ratom tree-id]
  (let [nav-vector (ti/tree-id->tree-path-nav-vector tree-id)
        my-cursor (r/cursor root-ratom nav-vector)]
    (when (seq (select-keys @my-cursor [:expanded]))
      (swap! my-cursor update :expanded not))))

(defn id-of-previous-sibling
  "Return the id of the previous sibling of this tree id. Returns nil if this
  tree id is the first (zero'th) in a group of siblings."
  [current-sibling-id]
  (let [parts (ti/tree-id->tree-id-parts current-sibling-id)
        last-path-index (int (nth parts (- (count parts) 2)))]
    (when (pos? last-path-index)
      (ti/tree-id-parts->tree-id-string
        (into (remove-last-two parts) [(dec last-path-index) "topic"])))))

(defn siblings-above
  "Return a (possibly empty) seq of siblings that appear higher in the tree
  display than the one denoted by the tree-id."
  [root-ratom tree-id]
  (loop [id (ti/decrement-leaf-index tree-id) res []]
    (if (nil? (get-topic root-ratom id))
      res
      (recur (ti/decrement-leaf-index id) (conj res id)))))

(defn siblings-below
  "Return a (possibly empty) seq of siblings that appear lower in the tree
  display than the one denoted by tree-id."
  [root-ratom tree-id]
  (loop [id (ti/increment-leaf-index tree-id) res []]
    (if (nil? (get-topic root-ratom id))
      res
      (recur (ti/increment-leaf-index id) (conj res id)))))

(defn id-of-last-visible-child
  "Return the id of the last visible child of the branch starting at tree-id.
  The last visible child may be many levels deeper in the tree."
  [root-ratom tree-id]
  (loop [id-so-far tree-id
         topic-map (get-topic root-ratom id-so-far)]
    (if-not (visible-children? topic-map)
      id-so-far
      (let [next-child-vector (:children topic-map)
            next-index (dec (count next-child-vector))
            next-topic (get next-child-vector next-index)
            next-id (ti/insert-child-index-into-parent-id id-so-far next-index)]
        (recur next-id next-topic)))))

(defn previous-visible-node
  "Return the tree id of the visible node one line up."
  [root-ratom current-node-id]
  (let [id-parts (remove-last (ti/tree-id->tree-id-parts current-node-id))
        last-part (js/parseInt (last id-parts))
        short-parts (remove-last id-parts)
        new-id (if (zero? last-part)
                 ; The first child under a parent.
                 (ti/tree-id-parts->tree-id-string (conj short-parts "topic"))
                 (id-of-last-visible-child
                   root-ratom
                   (ti/tree-id-parts->tree-id-string
                     (conj (conj short-parts (dec last-part)) "topic"))))]
    new-id))

(defn brute-force-next-visible-node
  "Return the next visible node in the tree after the current id.  Return
  nil if the tree-id already corresponds to the last visible node in the
  tree. This method should only be called if no simplifying conditions exist
  to identify the needed node more easily."
  [root-ratom tree-id]
  (when-not (bottom-visible-tree-id? root-ratom tree-id)
    (let [vis-nav-vector-seq (flatten-to-vectors
                               (tree->nav-vector-sequence
                                 @root-ratom []
                                 visible-children?))
          nav-parts (ti/numberize-parts (ti/tree-id->nav-index-vector tree-id))
          inc-matched-nav (inc (first (positions (fn [x] (= x nav-parts)) vis-nav-vector-seq)))
          complete-parts (conj (into ["root"] (nth vis-nav-vector-seq inc-matched-nav)) "topic")]
      (ti/tree-id-parts->tree-id-string complete-parts))))

(defn next-visible-node
  "Return the next visible node in the tree after the current node. Returns
  nil if the node is already the last visible node."
  [root-ratom current-node-id]
  (let [current-topic (get-topic root-ratom current-node-id)
        ; Pre-calculate one of the easy possibilities.
        next-sibling-id (ti/increment-leaf-index current-node-id)]
    (cond
      (visible-children? current-topic) (ti/id-of-first-child current-node-id)
      (get-topic root-ratom next-sibling-id) next-sibling-id
      :default (brute-force-next-visible-node root-ratom current-node-id))))

(defn remove-top-level-sibling!
  "Remove one of the top level topics from the tree. Return a copy of the
  branch (entire tree) with the sibling removed or nil if there was a problem
  with the arguments. Will not remove the last remaining top-level headline."
  [root-ratom sibling-index]
  (when (and (or (instance? reagent.ratom/RAtom root-ratom)
                 (instance? reagent.ratom/RCursor root-ratom))
             (vector @root-ratom)
             ; Don't delete the last remaining top-level topic.
             (> (count @root-ratom) 1)
             (>= sibling-index 0)
             (< sibling-index (count @root-ratom)))
    (swap! root-ratom delete-at sibling-index)))

(defn remove-child!
  "Remove the specified child from the parents vector of children. Return a
  copy of the branch with the child removed or nil if there was a problem
  with the arguments."
  [parent-ratom child-index]
  (when (or (instance? reagent.ratom/RAtom parent-ratom)
            (instance? reagent.ratom/RCursor parent-ratom))
    (let [vector-of-children (:children @parent-ratom)]
      (when (and vector-of-children
                 (vector? vector-of-children)
                 (>= child-index 0)
                 (< child-index (count vector-of-children)))
        (let [new-child-vector (delete-at vector-of-children child-index)]
          (if (empty? new-child-vector)
            (swap! parent-ratom dissoc :children)
            (swap! parent-ratom assoc :children new-child-vector)))))))

(defn prune-topic!
  "Remove the subtree with the given id from the tree. If the last child
  is deleted, the subtree is marked as having no children."
  ; THE RETURN VALUE IS INCONSISTENT HERE DEPENDING ON WHETHER A TOP LEVEL
  ; ITEM IS DELETED OR ONE LOWER IN THE TREE.
  [root-ratom id-of-existing-subtree]
  (let [path-and-index (ti/tree-id->nav-vector-and-index id-of-existing-subtree)
        parent-nav-index-vector (:path-to-parent path-and-index)
        child-index (:child-index path-and-index)]
    (if (empty? parent-nav-index-vector)
      (remove-top-level-sibling! root-ratom child-index)
      (let [child-vector-target (r/cursor root-ratom parent-nav-index-vector)]
        (remove-child! child-vector-target child-index)))))

;; This is such a dirty hack! It requires special handling if the first
;; argument is actually the root because the root is a vector, not a map.
;; It all boils down to the choice we made to make the root different so
;; we don't have an always present "root" node at the top of the control.
(defn add-child!
  "Insert the given topic at the specified index in the parents vector of
  children. Return a new copy of the parent that includes the new data."
  [parent-topic-ratom index topic-to-add]
  (if (vector? @parent-topic-ratom)
    (swap! parent-topic-ratom insert-at index topic-to-add)
    (let [child-topic-vector (or (:children @parent-topic-ratom) [])
          new-child-vector (insert-at child-topic-vector index topic-to-add)]
      (swap! parent-topic-ratom assoc :children new-child-vector))))

(defn graft-topic!
  "Add a new topic at the specified location in the tree. The topic is inserted
  into the tree. No data it removed. Any existing information at the location
  where the new data is grafted is pushed down in the tree."
  [root-ratom id-of-desired-node topic-to-graft]
  (let [path-and-index (ti/tree-id->nav-vector-and-index id-of-desired-node)]
    (add-child! (r/cursor root-ratom (:path-to-parent path-and-index))
                (:child-index path-and-index) topic-to-graft)))

(defn move-branch!
  "Move an existing branch to a new location."
  [root-ratom source-id destination-id]
  (let [topic-to-move (get-topic root-ratom source-id)
        id-to-focus (ti/change-tree-id-type destination-id "label")]
    (if (ti/lower? source-id destination-id)
      (do (prune-topic! root-ratom source-id)
          (graft-topic! root-ratom destination-id topic-to-move))
      (do (graft-topic! root-ratom destination-id topic-to-move)
          (prune-topic! root-ratom source-id)))
    (du/scroll-ele-into-view id-to-focus)))

(defn indent-branch!
  "Indent the given branch and return its new id. If the branch cannot be
  indented, return nil."
  [root-ratom branch-id]
  (when-not (ti/top-tree-id? branch-id)
    (when-let [previous-sibling (id-of-previous-sibling branch-id)]
      (expand-node! root-ratom previous-sibling)
      (let [sibling-child-count (count-children root-ratom previous-sibling)
            sibling-parts (ti/tree-id->tree-id-parts previous-sibling)
            with-added-leaf (conj (remove-last sibling-parts) sibling-child-count)
            demoted-prefix (ti/tree-id-parts->tree-id-string with-added-leaf)
            demoted-id (str demoted-prefix (ti/topic-separator) "topic")]
        (move-branch! root-ratom branch-id demoted-id)
        demoted-id))))

(defn outdent-branch!
  "Outdent (promote) the given branch and return its new id."
  [root-ratom branch-id]
  (when-not (ti/summit-id? branch-id)
    (let [parts (ti/tree-id->nav-index-vector branch-id)
          less-parts (remove-last parts)
          promoted-id (ti/increment-leaf-index (ti/nav-index-vector->tree-id-string less-parts))
          siblings-to-move (reverse (siblings-below root-ratom branch-id))]
      (when (seq siblings-to-move)
        (expand-node! root-ratom branch-id)
        (let [where-to-append (where-to-append-next-child root-ratom branch-id)]
          (loop [child (first siblings-to-move) siblings (rest siblings-to-move)]
            (when child
              (move-branch! root-ratom child where-to-append)
              (recur (first siblings) (rest siblings))))))
      (move-branch! root-ratom branch-id promoted-id)
      promoted-id)))

(defn outdent-all-children!
  "Outdent (promote) all the children of the given node."
  [root-ratom span-id & [children]]
  (let [child-array (or children (children? root-ratom span-id))
        first-id (ti/id-of-first-child span-id)]
    ;; Runs from the bottom child to the top. Doing it from top to bottom
    ;; would "capture" lower children under the higher children.
    (doseq [idx (range (dec (count child-array)) -1 -1)]
      (let [nxt-id (ti/set-leaf-index first-id idx)]
        (outdent-branch! root-ratom nxt-id)))
    span-id))

