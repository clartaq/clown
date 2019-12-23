(ns clown.client.tree-ids
  (:require [clown.client.util.dom-utils :as du]
            [clown.client.util.vector-utils :refer [delete-at remove-first
                                                    remove-last remove-last-two
                                                    insert-at replace-at
                                                    append-element-to-vector]]
            [clojure.string :as s]))

(defn topic-separator
  "A character that is unlikely to be typed in normal operation. In this case
  it is a half trianglular colon modifier character. Used as a separator when
  building path strings through the hierarchy."
  []
  \u02D1)

(defn root-parts
  "Returns a vector of the components used to build various ids of the root."
  []
  ["root" "0"])

(defn indent-increment
  "Return the amount of indentation (in rems) to add for each level
  down the tree."
  []
  1.5)

(defn new-topic
  "Return the map to be used for a new topic."
  []
  {:topic ""})

(defn convert-to-number-or-not
  "If every character in the string is a digit, convert the string to a
  number and return it. Otherwise, return the string unchanged."
  [s]
  (if (every? #(.includes "0123456789" %) s)
    (js/parseInt s)
    s))

;;------------------------------------------------------------------------------
;; Tree id manipulation functions. These are all basically string manipulation
;; functions that don't need to do anything with the data in the tree.

(defn tree-id->tree-id-parts
  "Split a DOM id string (as used in this program) into its parts and return
  a vector of the parts. Note that numeric indices in the parts vector are
  actually strings, not numbers."
  [id]
  (when (and id (seq id))
    (s/split id (topic-separator))))

(defn numberize-parts
  "Take a vector of tree id parts and convert any of the parts containing all
  digit characters to numbers. This can be useful with comparing parts vectors
  produced from tree ids to nav vectors produced by one of the tree
  traversal functions."
  [parts]
  (loop [p parts r []]
    (if (empty? p)
      r
      (recur (rest p) (conj r (convert-to-number-or-not (first p)))))))

(defn tree-id-parts->tree-id-string
  "Return a string formed by interposing the topic-separator between the
  elements of the input vector."
  [v]
  (when (and v (vector? v) (seq v))
    (str (s/join (topic-separator) v))))

(defn top-tree-id
  "Return the tree id for the top headline in any outline."
  []
  (tree-id-parts->tree-id-string (conj (root-parts) "topic")))

(defn top-tree-id?
  "Return true if tree-id represents to first sibling at the root level of
  the tree. (This topic is always displayed at the top of the tree -- hence
  the function name.)"
  [tree-id]
  (= (root-parts) (remove-last (tree-id->tree-id-parts tree-id))))

(defn id-of-first-child
  "Return the expected id of the first child of the node with this id. There
  is no guarantee that an actual tree node with the id exists."
  [tree-id]
  (let [id-parts (remove-last (tree-id->tree-id-parts tree-id))
        new-parts (conj (conj id-parts 0) "topic")]
    (tree-id-parts->tree-id-string new-parts)))

(defn nav-index-vector->tree-id-string
  "Creates a DOM id string from a vector of indices used to navigate to
  the topic. If no id type is specified, the default value of 'topic'
  is used."
  [nav-index-vector & [type-to-use]]
  (let [id-type (or type-to-use "topic")]
    (str "root" (topic-separator)
         (tree-id-parts->tree-id-string nav-index-vector)
         (topic-separator) id-type)))

(defn tree-id->nav-index-vector
  "Return a vector of the numeric indices in the child vectors from the
  root to the element id."
  [tree-id]
  (-> (tree-id->tree-id-parts tree-id)
      (remove-last)
      (remove-first)))

(defn summit-id?
  "Return true if tree-id represents a member of the top-level of topics."
  [tree-id]
  (= 1 (count (tree-id->nav-index-vector tree-id))))

(defn tree-id->parent-id
  "Return the id of the parent of this id or nil if the id is already a
  summit id. Returns nil if the tree-id is the top-most summit node."
  [tree-id]
  (when-not (summit-id? tree-id)
    (let [parts (tree-id->tree-id-parts tree-id)
          id-type (last parts)]
      (tree-id-parts->tree-id-string (conj (remove-last-two parts) id-type)))))

(defn tree-id->sortable-nav-string
  "Convert the element id to a string containing the vector indices
  separated by a hyphen and return it. Result can be used to lexicographically
  determine if one element is 'higher' or 'lower' than another in the tree."
  [tree-id]
  (s/join "-" (tree-id->nav-index-vector tree-id)))

(defn set-leaf-index
  "Return a new version of the tree-id where the leaf index has been set to
  a new value."
  [tree-id new-index]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        new-parts (replace-at parts index-in-vector new-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn increment-leaf-index-by
  "Given the tree id of a leaf node, return a version of the same
  tree id with the leaf index incremented by the given value."
  [tree-id by]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        new-index (+ (int (nth parts index-in-vector)) by)
        new-parts (replace-at parts index-in-vector new-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn increment-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  incremented."
  [tree-id]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        inc-index (inc (int (nth parts index-in-vector)))
        new-parts (replace-at parts index-in-vector inc-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn decrement-leaf-index
  "Given the tree id of a leaf node, return an id with the node index
  decremented. Can produce leaf indices < 0, which some functions
  depend on."
  [tree-id]
  (let [parts (tree-id->tree-id-parts tree-id)
        index-in-vector (- (count parts) 2)
        dec-index (dec (int (nth parts index-in-vector)))
        new-parts (replace-at parts index-in-vector dec-index)]
    (tree-id-parts->tree-id-string new-parts)))

(defn change-tree-id-type
  "Change the 'type' of a tree DOM element id to something else."
  [id new-type]
  (let [parts (tree-id->tree-id-parts id)
        shortened (remove-last parts)]
    (str (tree-id-parts->tree-id-string shortened) (str (topic-separator) new-type))))

(defn insert-child-index-into-parent-id
  "Return a new id where the index of the child in the parents children vector
  has been appended."
  [parent-id child-index]
  (-> (tree-id->tree-id-parts parent-id)
      (remove-last)
      (conj child-index)
      (conj "topic")
      (tree-id-parts->tree-id-string)))

(defn tree-id->tree-path-nav-vector
  "Return a vector of indices and keywords to navigate to the piece of data
  represented by the DOM element with the given id."
  [tree-id]
  (let [nav-vector (mapv int (tree-id->nav-index-vector tree-id))
        interposed (interpose :children nav-vector)]
    (vec interposed)))

(defn tree-id->nav-vector-and-index
  "Parse the id into a navigation path vector to the parent of the node and an
  index within the vector of children. Return a map containing the two pieces
  of data. Basically, parse the id into a vector of information to navigate
  to the parent (a la get-in) and the index of the child encoded in the id."
  [tree-id]
  (let [string-vec (tree-id->tree-id-parts tree-id)
        idx (int (nth string-vec (- (count string-vec) 2)))
        without-last-2 (remove-last-two string-vec)
        without-first (delete-at without-last-2 0)
        index-vector (mapv int without-first)
        interposed (interpose :children index-vector)]
    {:path-to-parent (vec interposed) :child-index idx}))

(defn editing?
  "Return true if the element with the given id is in the editing state."
  [id]
  (let [editor-id (change-tree-id-type id "editor")]
    (= (du/style-property-value editor-id "display") "initial")))

(defn lower?
  "Return true if the first path is 'lower' in the tree than second path."
  [first-path second-path]
  (pos? (compare (tree-id->sortable-nav-string first-path)
                 (tree-id->sortable-nav-string second-path))))

(defn tree-id-and-index->note-title-id
  [tree-id index]
  (change-tree-id-type tree-id (str "note-title-id-" index)))

(defn tree-id-and-index->note-id
  [tree-id index]
  (change-tree-id-type tree-id (str "note-id-" index)))

