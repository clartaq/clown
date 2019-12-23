;;;;
;;;; The functions in this namespace are used to focus, scroll, and highlight
;;;; various editor elements in the clown interface.
;;;;

(ns clown.client.util.focus-utils
  (:require [clown.client.util.dom-utils :as du]
            [clown.client.tree-ids :as ti]))

(defn highlight-and-scroll-editor-for-id
  "Focus the editor associated with the id (assumes that the label associated
  with the id is visible). If needed scroll the editor into view. Select
  the region represented by begin-highlight and end-highlight."
  [tree-id begin-highlight end-highlight]
  (when tree-id
    (let [editor-id (ti/change-tree-id-type tree-id "editor")
          editor-ele (du/get-element-by-id editor-id)]
      (when-not (ti/editing? editor-id)
        (let [label-id (ti/change-tree-id-type tree-id "label")]
          (du/swap-display-properties label-id editor-id)))
      (du/focus-element editor-ele)
      (du/scroll-ele-into-view editor-id)
      (du/set-selection-range editor-ele begin-highlight end-highlight))))

(defn focus-and-scroll-editor-for-id
  "Focus the editor associated with the id (assumes that the label associated
  with the id is visible). If needed, scroll the editor into view. If a caret
  position is provided, place the editor caret at that position."
  [tree-id & [caret-pos]]
  (when tree-id
    (let [editor-id (ti/change-tree-id-type tree-id "editor")
          editor-ele (du/get-element-by-id editor-id)]
      (when-not (ti/editing? editor-id)
        (let [label-id (ti/change-tree-id-type tree-id "label")]
          (du/swap-display-properties label-id editor-id)))
      (du/focus-element editor-ele)
      (du/scroll-ele-into-view editor-id)
      (when caret-pos
        (du/set-selection-range editor-ele caret-pos caret-pos)))))

