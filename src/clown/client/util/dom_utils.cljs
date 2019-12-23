;;;;
;;;; DOM related interop.
;;;;
;;;; Besides moving related functions out of the main program file, moving
;;;; them here minimizes the noise from Cursive about unresolvable functions.
;;;;

(ns clown.client.util.dom-utils)

(defn get-element-by-id
  [id]
  (.getElementById js/document id))

(defn event->target-element
  [evt]
  (.-target evt))

(defn event->target-id
  [evt]
  (.-id (event->target-element evt)))

(defn ele->value
  [ele]
  (.-value ele))

(defn event->target-value
  [evt]
  (ele->value (event->target-element evt)))

(defn active-element-id
  []
  (.-id (.-activeElement js/document)))

(defn set-class-name!
  [ele name]
  (set! (.-className ele) name))

(defn stop-propagation
  [evt]
  (.stopPropagation evt))

(defn prevent-default
  [evt]
  (.preventDefault evt))

(defn event-data
  [evt]
  (.-data evt))

(defn all-digits?
  [s]
  (every? #(.includes "0123456789" %) s))

(defn create-dom-element
  [kind]
  (.createElement js/document kind))

(defn get-canvas-context
  [canvas kind]
  (.getContext canvas kind))

(defn set-context-font!
  [context font-name-string]
  (set! (.-font context) font-name-string))

(defn measure-text-width
  [context text]
  (.-width (.measureText context text)))

(defn get-caret-position
  "Return the caret position of the text element with the id passed."
  [ele-id]
  (.-selectionStart (get-element-by-id ele-id)))

(defn selection-start
  [ele-id]
  (.-selectionStart (get-element-by-id ele-id)))

(defn selection-end
  [ele-id]
  (.-selectionEnd (get-element-by-id ele-id)))

(defn set-selection-range
  [element start-pos end-pos]
  (.setSelectionRange element start-pos end-pos))

(defn focus-offset
  []
  (.-focusOffset (.getSelection js/window)))

(defn focus-element
  [ele]
  (.focus ele))

(defn value-length
  [ele]
  (.-length (.-value ele)))

(defn ele-in-visible-area?
  "Return non-nil if the element is within the visible area of the
  scroll port. May not be visible due to CSS settings or if its parent
  is in a collapsed state. NOTE that, unlike many of the functions in
  this section, this function expects a DOM element, not an id."
  [ele]
  (let [r (.getBoundingClientRect ele)
        doc-ele (.-documentElement js/document)
        wdw-height (or (.-innerHeight js/window) (-.clientHeight doc-ele))
        wdw-width (or (.-innerWidth js/window) (.-clientWidth doc-ele))
        result (and (>= (.-left r) 0) (>= (.-top r) 0)
                    (<= (+ (.-left r) (.-width r)) wdw-width)
                    (<= (+ (.-top r) (.-height r)) wdw-height))]
    result))

(defn scroll-ele-into-view
  "Scroll the element with the given id into view. Note: This must be the id
  of a DOM element, not an element in the data tree."
  [ele-id]
  (when-let [ele (get-element-by-id ele-id)]
    (when-not (ele-in-visible-area? ele)
      (.scrollIntoView ele))))

(defn get-style
  "Return the value of the style rule for the given element."
  [ele rule]
  (-> js/document
      .-defaultView
      (.getComputedStyle ele "")
      (.getPropertyValue rule)))

(defn style-property-value
  "Return the value of the property for the element with the given id."
  [id property]
  (when-let [style-declaration (.-style (get-element-by-id id))]
    (.getPropertyValue style-declaration property)))

(defn swap-style-property
  "Swap the specified style settings for the two elements."
  [first-id second-id property]
  (let [style-declaration-of-first (.-style (get-element-by-id first-id))
        style-declaration-of-second (.-style (get-element-by-id second-id))
        value-of-first (.getPropertyValue style-declaration-of-first property)
        value-of-second (.getPropertyValue style-declaration-of-second property)]
    (.setProperty style-declaration-of-first property value-of-second)
    (.setProperty style-declaration-of-second property value-of-first)))

(defn swap-display-properties
  "Swap the display style properties for the two elements."
  [first-id second-id]
  (swap-style-property first-id second-id "display"))

(defn resize-textarea
  "Resize the element vertically."
  [text-id]
  (when-let [ele (get-element-by-id text-id)]
    (let [style (.-style ele)]
      (set! (.-overflow style) "hidden")
      (set! (.-height style) "5px")
      (set! (.-height style) (str (.-scrollHeight ele) "px")))))

(defn key-evt->map
  "Unpack the information in a keyboard event into a map that can be used
  easily to dispatch the event to a handler"
  [evt]
  {:key       (.-key evt)
   :modifiers {:ctrl  (.-ctrlKey evt)
               :shift (.-shiftKey evt)
               :alt   (.-altKey evt)
               :cmd   (.-metaKey evt)}})

(defn add-event-listener
  [event-name f]
  (.addEventListener js/document event-name f))

