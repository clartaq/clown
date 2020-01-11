(ns clown.client.layout
  (:require [clown.client.buttons :as bn]
            [clown.client.util.dialogs :as dlg]
            [clown.client.util.dom-utils :as du]
            [clown.client.util.dragging :refer [drag-click-handler]]
            [clown.client.util.font-detection :as fd]
            [clown.client.util.focus-utils :as fu]
            [clown.client.util.mru :as mru]
            [clown.client.tree-ids :as ti]
            [clown.client.tree-manip :as tm]
            [clown.client.util.undo-redo :as ur]
            [clown.client.util.vector-utils :refer [delete-at remove-first
                                                    remove-last remove-last-two
                                                    insert-at replace-at
                                                    append-element-to-vector]]
            [reagent.core :as r]
            [taoensso.timbre :refer [tracef debugf infof warnf errorf
                                     trace debug info warn error]]))

(defn indent-increment
  "Return the amount of indentation (in rems) to add for each level
  down the tree."
  []
  1.5)

;;------------------------------------------------------------------------------
;; Functions for layout of notes.

(defn notes?
  "Return the entire vector of notes if present, nil otherwise."
  [root-ratom id]
  (:notes (tm/get-topic root-ratom id)))

(defn count-notes
  "Return the number of notes of the topic."
  [root-ratom id]
  (count (notes? root-ratom id)))

(defn add-completely-new-note-section
  "Add a new note section and a new note."
  [aps topic-cursor]
  (debug "add-completely-new-note-section")
  (let [prefs (:preferences @aps)
        def-title (:default_new_note_name prefs)
        def-text (:default_new_note_text prefs)]
    (swap! topic-cursor merge {:last-note-viewed 0
                               :notes            [{:note-title def-title
                                                   :note       def-text}]})))

(defn add-to-existing-note-section
  "Add a new note to a pre-existing note section in the topic."
  [aps topic-cursor ntf]
  (debug "add-to-existing-note-section")
  (let [prefs (:preferences @aps)
        def-title (:default_new_note_name prefs)
        def-text (:default_new_note_text prefs) notes-vector (:notes @topic-cursor)
        new-notes (conj notes-vector {:note-title def-title
                                      :note       def-text})]
    (swap! topic-cursor assoc :last-note-viewed ntf)
    (swap! topic-cursor assoc :notes new-notes)))

(defn add-new-note
  "Add a new note to the topic. The new note is always the last one."
  [aps root-ratom topic-id]
  (let [topic-nav (ti/tree-id->tree-path-nav-vector topic-id)
        topic-cursor (r/cursor root-ratom topic-nav)
        ntf (count-notes root-ratom topic-id)
        note-title-id (ti/tree-id-and-index->note-title-id topic-id ntf)]
    (if (notes? root-ratom topic-id)
      (add-to-existing-note-section aps topic-cursor ntf)
      (add-completely-new-note-section aps topic-cursor))
    ;; Now hightlight the title of the new note, ready for typing.
    (r/after-render (fn []
                      (when-let [title-ele (du/get-element-by-id note-title-id)]
                        (du/focus-element title-ele)
                        (when-let [active-id (du/active-element-id)]
                          (when (= active-id note-title-id)
                            (let [ele (du/get-element-by-id note-title-id)
                                  len (du/value-length ele)]
                              (du/set-selection-range ele 0 len)))))))))

(defn delete-note
  "Delete the note specified and move the view to the previous note. In the
  event that there are no remaining notes, the :notes and :last-note-viewed
  elements are removed as well."
  [root-ratom topic-id index]
  (debugf "delete-note: topic-id: %s, index: %s" topic-id index)
  (let [topic-nav (ti/tree-id->tree-path-nav-vector topic-id)
        topic-cursor (r/cursor root-ratom topic-nav)
        notes-cursor (r/cursor topic-cursor [:notes])]
    (swap! notes-cursor delete-at index)
    (debugf "     (notes? root-ratom topic-id): %s" (notes? root-ratom topic-id))
    (if (zero? (count-notes root-ratom topic-id))
      (do
        (swap! topic-cursor dissoc :last-note-viewed)
        (swap! topic-cursor dissoc :notes))
      (swap! topic-cursor assoc :last-note-viewed (max 0 (dec index))))))

(defn layout-add-note-button
  "Return a function that will layout a button for adding a new note for
  the given headline (label)."
  [aps root-ratom label-id-ratom]
  (fn [aps root-ratom label-id-ratom]
    [:div.info-item.last
     [:div.note-tab-control--add-note-button
      {:role "add-note-tab"}
      [:span.note-tab-control--add-note-icon
       {:title   "Add a new note"
        :onClick (fn [evt]
                   (add-new-note aps root-ratom @label-id-ratom)
                   (du/stop-propagation evt))}
       ;; Symbol is a plus sign.
       "\u002b"]]]))

(defn layout-note-title
  [subtree-ratom topic-id index]
  (debugf "layout-note-title: topic-id: %s, index: " topic-id index)
  (let [canvas (atom nil)
        context (atom nil)
        font-style (atom nil)
        title-id (ti/tree-id-and-index->note-title-id topic-id index)]

    (r/create-class
      {
       :name                "layout-title-editor"

       :component-did-mount (fn [this]
                              (debugf "    component-did-mount: index: %s" index)
                              (when-let [ele (du/get-element-by-id title-id)]
                                (when (nil? @canvas)
                                  (reset! canvas (du/create-dom-element "canvas"))
                                  (reset! context (du/get-canvas-context @canvas "2d"))
                                  (let [ff (du/get-style ele "font-family")
                                        font-to-use (fd/font-family->font-used ff)
                                        fs (du/get-style ele "font-size")
                                        ff-str (str fs " " font-to-use)]
                                    (reset! font-style ff-str)
                                    (du/set-context-font! @context ff-str)
                                    ;; Force another render now that we can measure
                                    ;; correctly. This has the effect of correctly
                                    ;; displaying even some pathological tags, like
                                    ;; one containing many "W"s or other wide
                                    ;; characters for example.
                                    (r/after-render #(r/force-update this))))))

       :reagent-render      (fn [subtree-ratom]
                              (debugf "    reagent-render: @subtree-ratom: %s" @subtree-ratom)
                              (let [title-r (r/cursor subtree-ratom [:notes index :note-title])
                                    _ (debugf "    title-r: %s" title-r)
                                    last-note-viewed (:last-note-viewed @subtree-ratom)
                                    _ (debugf "    last-note-viewed: %s, index: %s" last-note-viewed index)
                                    class-to-use (if (= index last-note-viewed)
                                                   "note-tab-control--title-input note-tab-control--title-input-is-selected"
                                                   "note-tab-control--title-input note-tab-control--title-input-is-not-selected")

                                    ch-cnt (count @title-r)
                                    str-width (if (zero? ch-cnt)
                                                ; Make sure a cursor is visible.
                                                "5px"
                                                (if (and @canvas @context)
                                                  ; Use real measurement. Add
                                                  ; about 10px to account for the
                                                  ; editing cursor pushing text
                                                  ; aside.
                                                  (str (+ 10 (du/measure-text-width
                                                               @context @title-r)) "px")
                                                  ; Otherwise, use a "pretty
                                                  ; close" measure.
                                                  (str ch-cnt "ch")))]
                                (debugf "    str-width: %s" str-width)
                                [:input {:type         "text"
                                         :tabIndex     0
                                         :autoComplete "off"
                                         :style        {:width     str-width
                                                        ; Firefox also requires
                                                        ; max-width to work
                                                        :max-width str-width}
                                         :class        class-to-use
                                         :id           title-id
                                         :value        @title-r
                                         :on-focus     #(swap! subtree-ratom
                                                               assoc :last-note-viewed index)
                                         :on-change    #(reset! title-r
                                                                (du/event->target-value %))}]))})))

(defn layout-tab-item
  "Layout and return the HTML for a single tab. If it is the selected tab, it
  is assigned a CSS class to give it an appearance distinct from any others others."
  [root-ratom subtree-ratom topic-id-ratom index]
  (debugf "layout-tab-item: @subtree-ratom: %s" @subtree-ratom)
  (fn [root-ratom subtree-ratom topic-id-ratom]
    (let [last-note-viewed (:last-note-viewed @subtree-ratom)
          class-to-use (if (= index last-note-viewed)
                         "note-tab-control--tab note-tab-control--tab-is-selected"
                         "note-tab-control--tab note-tab-control--tab-is-not-selected")]
      [:div.note-tab-control--tab
       {:role    "tab"
        :class   class-to-use
        :onClick #(swap! subtree-ratom assoc :last-note-viewed index)}

       [layout-note-title subtree-ratom @topic-id-ratom index]

       [:span.note-tab-control--delete-note-icon
        {:title   "Delete this note"
         :onClick (fn [evt]
                    (delete-note root-ratom @topic-id-ratom index)
                    (du/stop-propagation evt))}
        ;; Symbol is a multiplication symbol.
        "\u00d7"]])))

(defn title-tab-list
  "Return an (html) unordered list of note titles for the notes."
  [root-ratom subtree-ratom headline-id-ratom]
  (debug "title-tab-list")
  (let [notes (notes? root-ratom @headline-id-ratom)]
    (let [user-tabs (into [:div.note-tab-control--list {:role "tab-list"}]
                          (map-indexed (fn [index _]
                                         [layout-tab-item root-ratom subtree-ratom
                                          headline-id-ratom index]) notes))]
      user-tabs)))

(defn make-note-area
  "Layout an individual note."
  [subtree-ratom headline-id-ratom note-index]
  (debugf "make-note-area: @headline-id-ratom: %s, note-index: %s" @headline-id-ratom note-index)
  (let [note-ratom (r/cursor subtree-ratom [:notes note-index :note])
        last-note-viewed (or (:last-note-viewed @subtree-ratom) 0)]
    (debugf "note-index: %s, @headline-id-ratom: %s" note-index @headline-id-ratom)
    (debugf "@subtree-ratom:%s" @subtree-ratom)
    (debugf "@note-ratom: %s" @note-ratom)
    (debugf "last-note-viewed: %s" last-note-viewed)

    [:textarea.note-tab-control--inner-panel
     {:role     "tab-panel"
      :value    @note-ratom
      :style    {:display (if (= note-index last-note-viewed)
                            :block
                            :none)}
      :onChange #(reset! note-ratom (du/event->target-value %))}]))

(def note-areas-div-id "note-areas-div-id")

(defn note-areas
  "Layout the notes for the note panel."
  [root-ratom subtree-ratom headline-id-ratom]
  (debugf "note-areas: @headline-id-ratom: %s" @headline-id-ratom)
  (when (notes? root-ratom @headline-id-ratom)
    (let [headline-id @headline-id-ratom]
      [into [:div.note-tab-control--notes-div
             {:id note-areas-div-id}
             (for [note-index (range (count-notes root-ratom headline-id))]
               ^{:key (ti/tree-id-and-index->note-id headline-id note-index)}
               [make-note-area subtree-ratom headline-id-ratom note-index])]])))

(defn layout-note-panel
  "Layout the note panel for the selected headline."
  [root-ratom headline-id-ratom]
  (debugf "layout-note-panel: @headline-id-ratom: %s" @headline-id-ratom)
  (let [nav-path (ti/tree-id->tree-path-nav-vector @headline-id-ratom)
        subtree-ratom (r/cursor root-ratom nav-path)]
    [:div {:class "note-tab-control--panel"
           :role  "note-tab-control--panel"}
     [title-tab-list root-ratom subtree-ratom headline-id-ratom]
     [note-areas root-ratom subtree-ratom headline-id-ratom]]))

;;;-----------------------------------------------------------------------------
;;; The rest of the interface.

(defn handle-chevron-click!
  "Handle the click on the expansion chevron by toggling the state of
  expansion in the application state atom. This will cause the tree
  to re-render visually."
  [root-ratom evt]
  (let [ele-id (du/event->target-id evt)
        kwv (ti/tree-id->tree-path-nav-vector ele-id)
        ekwv (conj kwv :expanded)]
    (swap! root-ratom update-in ekwv not)
    (fu/focus-and-scroll-editor-for-id ele-id)))

(defn html-collection->seq
  "Return a vector of elements taken from the HTMLCollection."
  [coll]
  (let [res-atom (atom [])]
    (doseq [idx (range (du/collection-length coll))]
        (swap! res-atom conj (du/collection-item coll idx)))
    @res-atom))

(defn handle-note-icon-click!
  "Respond to a click on a note icon by making the notes for the headline
  visible, then placing the caret in the last note viewed."
  [aps evt]
  (let [ele-id (du/event->target-id evt)
        focused-headline-ratom (r/cursor aps [:focused-headline])]
    ;; Resetting the focused headline will make the associated notes visible.
    (reset! focused-headline-ratom ele-id)
    (r/after-render
      (fn []
        (let [div-ele (du/get-element-by-id note-areas-div-id)
              div-children (du/children div-ele)
              ele-seq (html-collection->seq div-children)
              vis-ele (first (filterv #(= "block" (du/get-style % "display")) ele-seq))]
          ;; Some browsers require that you focus the element first.
          (du/focus-element vis-ele)
          (du/set-selection-range vis-ele 0 0))))))

(defn note-row-icon-div
  "Return a div that shows a 'note' icon in the gutter if the headline has
  any notes associated with it."
  [aps note-id note-icon-visible]
  (let [style (if note-icon-visible
                {:flex "0 0 1.5rem"}
                {:flex "0 0 1.5rem" :opacity "0"})]
    ^{:key note-id}
    [:div.tree-control--note-row-icon-div
     {:id       note-id :style style
      :title     "Edit the last note viewed for this headline."
      :on-click #(handle-note-icon-click! aps %)}]))

(defn indent-div
  "Return a div that provides the appropriate indentation for the headline
  at this level in the outline."
  [indent-id]
  (let [id-v (ti/tree-id->nav-index-vector indent-id)
        indent (* (indent-increment) (dec (count id-v)))
        indent-style (str 0 " " 0 " " indent "rem")]
    ^{:key indent-id}
    [:div.tree-control--indent-div {:id indent-id :style {:flex indent-style}}]))

(defn chevron-div
  "Get the expansion symbol to be used at the front of a topic. Returns
  a result based on whether the tree has children, and if so, whether they
  are expanded or not."
  [root-ratom subtree-ratom chevron-id]
  (let [want-bullets true
        bullet-opacity (if want-bullets "0.7" "0.0")
        base-attrs {:class "tree-control--chevron-div"
                    :id    chevron-id}
        clickable-chevron-props (merge base-attrs
                                       {:on-click #(handle-chevron-click! root-ratom %)})
        invisible-chevron-props (merge base-attrs {:style {:opacity bullet-opacity}})
        es (cond
             (tm/visible-children? @subtree-ratom) [:div clickable-chevron-props
                                                    (str \u25BC \space)]
             (:children @subtree-ratom) [:div clickable-chevron-props
                                         (str \u25BA \space)]
             ; Headlines with no children can be displayed with or without
             ; a bullet depending on the setting of "want-bullets" above.
             :default [:div invisible-chevron-props (str \u25cf \space)])]
    es))

(defn handle-info-div-click
  "Handle a click on an info-div by swapping it to an editor,
  grabbing the focus, and placing the cursor. Then populate the
  notes pane with any notes."
  [root-ratom editor-id label-id]
  (let [ed-ele (du/get-element-by-id editor-id)
        ofs (du/focus-offset)]
    (du/swap-display-properties label-id editor-id)
    (du/focus-element ed-ele)
    (du/set-selection-range ed-ele ofs ofs)))

(defn topic-info-div
  "Build the textual/interactive part of a topic/headline."
  [aps root-ratom sub-tree-ratom ids-for-row]
  (let [topic-ratom (r/cursor sub-tree-ratom [:topic])
        label-id (:label-id ids-for-row)
        editor-id (:editor-id ids-for-row)
        topic-id (:topic-id ids-for-row)
        focused-headline-ratom (r/cursor aps [:focused-headline])
        key-down-handler (:outline-key-down-handler @aps)]
    [:div.tree-control--topic-info-div
     [:label.tree-control--topic-label
      {:id      label-id
       :style   {:display :initial}
       :for     editor-id
       ; debugging
       ;:onMouseOver #(println "topic-id: " topic-id ", label-id: " label-id ", editor-id: " editor-id)
       :onClick #(handle-info-div-click root-ratom editor-id label-id)}
      @topic-ratom]

     [:textarea.tree-control--topic-editor
      {:id           editor-id
       :style        {:display :none :resize :none}
       :autoComplete "off"
       :onKeyDown    #(key-down-handler root-ratom % topic-ratom topic-id)
       :onKeyUp      #(du/resize-textarea editor-id)
       :onFocus      (fn on-focus [_]
                       ; Override default number of rows (2).
                       (du/resize-textarea editor-id)
                       (reset! focused-headline-ratom editor-id))
       :onBlur       #(when (= editor-id (du/active-element-id))
                        (du/swap-display-properties label-id editor-id))
       :on-change    #(reset! topic-ratom (du/event->target-value %))
       :value        @topic-ratom}]]))

(defn dom-ids-for-row
  "Return a map of all of the ids used in building a row of the control."
  [parts]
  (let [row-id-parts (conj parts "row")
        row-id (ti/tree-id-parts->tree-id-string row-id-parts)]
    {:row-id           row-id
     :note-row-icon-id (ti/change-tree-id-type row-id "note-icon")
     :indent-id        (ti/change-tree-id-type row-id "indent")
     :chevron-id       (ti/change-tree-id-type row-id "chevron")
     :topic-id         (ti/change-tree-id-type row-id "topic")
     :label-id         (ti/change-tree-id-type row-id "label")
     :editor-id        (ti/change-tree-id-type row-id "editor")}))

(defn outliner-row-div
  "Return one row of the outliner."
  [aps root-ratom index-vector]
  (let [ids-for-row (dom-ids-for-row index-vector)
        note-icon-visible (notes? root-ratom (:topic-id ids-for-row))
        row-id (:row-id ids-for-row)
        note-row-icon-div-id (:note-row-icon-id ids-for-row)
        indent-id (:indent-id ids-for-row)
        chevron-id (:chevron-id ids-for-row)
        nav-path (ti/tree-id->tree-path-nav-vector row-id)
        subtree-ratom (r/cursor root-ratom nav-path)]
    ^{:key row-id}
    [:div.tree-control--row-div
     [note-row-icon-div aps note-row-icon-div-id note-icon-visible]
     [indent-div indent-id]
     [chevron-div root-ratom subtree-ratom chevron-id]
     [topic-info-div aps root-ratom subtree-ratom ids-for-row]]))

(defn tree->hiccup
  "Return a div containing all of the visible content of the tree based on
  the current state of the tree."
  [aps root-ratom]
  (fn [aps root-ratom]
    (debugf "tree->hiccup: @root-ratom: %s" @root-ratom)
    (let [nav-vectors (tm/visible-nodes @root-ratom ["root"])]
      (into [:div.tree-control--list]
            (map #(outliner-row-div aps root-ratom %) nav-vectors)))))

(defn layout-outliner
  "Return a function that lays out the main content of the app."
  [aps]
  (debug "layout-outliner")
  (debugf "    @aps: %s" @aps)
  (let [root-ratom (r/cursor aps [:current-outline :tree])
        um (ur/undo-manager root-ratom)
        focused-headline-ratom (r/cursor aps [:focused-headline])
        outline-container-id "outline-container-id"
        title-container-id "title-container-id"
        note-container-id "note-container-id"
        file-name-container-id "file-name-container-id"]
    (swap! aps assoc :undo-redo-manager um)
    (debugf "    @aps with undo manager: %s" @aps)

    (fn [aps]
      (debug "    layout-outliner interior function")
      (debugf "    @aps: %s" @aps)
      (let [file-name (first (get-in @aps [:preferences :mru]))
            outline-title (get-in @aps [:current-outline :title])
            outliner-width (get-in @aps [:preferences :outline_width])
            notes-width (get-in @aps [:preferences :note_width])
            author-name (get-in @aps [:current-outline :author])
            created (get-in @aps [:current-outline :created])
            modified (get-in @aps [:current-outline :modified])]
        (debugf "    outliner-width: %s" outliner-width)
        (debugf "    notes-width: %s" notes-width)
        (debugf "    @root-ratom: %s" @root-ratom)
        [:div.Site
         [:header.Site-header {:role "banner"}
          [:div.banner
           [:div.banner--title-container {:id    title-container-id
                                          :style {:flex-basis outliner-width}}
            [:div.banner--title-label [:p "Title:"]]
            [:div.banner--title-editor
             [:input {:type      "text" :placeholder "Unknown" :value (if outline-title
                                                                        outline-title
                                                                        "")
                      :on-change #(swap! aps assoc-in [:current-outline :title]
                                         (du/event->target-value %))}]]]

           [:div.banner--spacer]

           [:div.banner--file-name-container {:id    file-name-container-id
                                              :style {:flex-basis notes-width}}
            [:div.banner--file-name-label [:p "File:"]]
            [:div.banner--file-name-editor
             [:input {:type      "text" :placeholder "Unknown" :spell-check "false"
                      :value     (if file-name
                                   file-name
                                   "")
                      :on-change #(mru/replace-first-item!
                                    aps
                                    (du/event->target-value %))}]]]]]

         [:main.Site-content {:id "Site-content"}
          [:div.outline-container {:id    outline-container-id
                                   :style {:flex-basis outliner-width}}

           [:div.note-info-column
            [:div.info-column
             [:div.info-row
              [:div.info-item
               (if author-name
                 (str "Author: " author-name)
                 "Author: Unknown")]]
             [:div.info-row
              [:div.info-item
               (if created
                 (str "Created: " created)
                 "Created: Unknown")]]
             [:div.info-row
              [:div.info-item
               (if modified
                 (str "Modified: " modified)
                 "Modified: Unknown")]]
             [:div.info-row
              [:div.info-item
               "Headlines: " (tm/count-nodes @root-ratom)]]]]

           [:div.tree-control--container-div
            {:onKeyDown #((:outline-container-key-down-handler @aps)
                          % root-ratom um)}
            [tree->hiccup aps root-ratom]]]

          ;; Draggable border for changing the width of the outliner area.
          [:div {:class "vertical-page-divider"}]
          [:div {:class       "vertical-page-splitter"
                 :id          "outline-drag-border"
                 :onMouseDown #(drag-click-handler @aps
                                                   [outline-container-id
                                                    title-container-id]
                                                   :outline_width)}]

          ;; Notes area.
          [:div.note-container {:id    note-container-id
                                :style {:flex-basis notes-width}}

           [:div.note-info-column
            [:div.info-column
             [:div.info-row
              [:div.info-item
               (if @focused-headline-ratom
                 (:topic (tm/get-topic root-ratom @focused-headline-ratom))
                 "note-container")]
              (when @focused-headline-ratom
                [layout-add-note-button aps root-ratom focused-headline-ratom])]]]

           [:div {:class "note-tab-control--container-div"
                  :id    "note-tab-control--container-div"}
            (when-not @focused-headline-ratom
              (reset! focused-headline-ratom (ti/top-tree-id)))
            [layout-note-panel root-ratom focused-headline-ratom]]]

          ;; Draggable border for changing the width of the notes area.
          [:div {:class "vertical-page-divider"}]
          [:div {:class       "vertical-page-splitter"
                 :id          "note-drag-border"
                 :onMouseDown #(drag-click-handler @aps
                                                   [note-container-id
                                                    file-name-container-id]
                                                   :note_width)}]]

         ;; Footer stuff.
         [bn/add-buttons aps]
         ;; Would this be a good place to add any "Cornell Summary" type stuff.
         ;; Hide it for now, but keep it around until ideas settle a bit.
         [:footer.Site-footer {:style {:display "none"}}
          [:div.Footer "clown footer"]]

         ;; Dialog stuff. Hidden until used.
         [dlg/layout-file-read-error-dlg]
         [dlg/layout-bad-edn-modal]
         [dlg/layout-bad-outline-modal]
         [dlg/layout-about-to-update-mru-modal]
         [dlg/layout-file-does-not-exist-dlg]
         [:div {:class "modal-overlay closed" :id "modal-overlay"}]]))))

