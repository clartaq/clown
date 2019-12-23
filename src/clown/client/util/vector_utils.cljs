;;;;
;;;; Some convenience functions for working with vectors.
;;;;

(ns clown.client.util.vector-utils)

(enable-console-print!)

(defn delete-at
  "Remove the nth element from the vector and return the result."
  [v n]
  (into (subvec v 0 n) (subvec v (inc n))))

(defn remove-first
  "Remove the first element in the vector and return the result."
  [v]
  (subvec v 1))

(defn remove-last
  "Remove the last element in the vector and return the result."
  [v]
  (subvec v 0 (dec (count v))))

(defn remove-last-two
  "Remove the last two elements in the vector and return the result."
  [v]
  (subvec v 0 (- (count v) 2)))

(defn insert-at
  "Return a copy of the vector with new-item inserted at the given n. If
  n is less than zero, the new item will be inserted at the beginning of
  the vector. If n is greater than the length of the vector, the new item
  will be inserted at the end of the vector."
  [v n new-item]
  (cond (neg? n) (into [new-item] v)
        (>= n (count v)) (conj v new-item)
        :default (into (conj (subvec v 0 n) new-item) (subvec v n))))

(defn prepend
  "Return a copy of the vector with the new item in the first (zero-th)
  position. Just a convenience function for '(insert-at v 0 new-item)."
  [v new-item]
  (insert-at v 0 new-item))

(defn replace-at
  "Replace the current element in the vector at index with the new-element
  and return it."
  [v index new-element]
  (insert-at (delete-at v index) index new-element))

(defn append-element-to-vector
  "Reaturn a copy of the vector with the new element appended to the end."
  [v new-item]
  (into [] (concat v [new-item])))

(defn positions
  "Return a vector of the index positions of elements in coll that satisfy pred."
  [pred coll]
  (vec (keep-indexed #(when (pred %2) %1) coll)))

(defn find-and-delete-all
  "Return a copy of v from which all elements matching elt have been removed."
  [v elt]
  (cond
    (nil? v) nil
    (nil? elt) v
    :default
    (vec (filter #(not= elt %) v))))

(defn prepend-uniquely
  "Return a copy of vu with new-element in the first position and any other
  copies of the element removed."
  [v new-element]
  (prepend (find-and-delete-all v new-element) new-element))

