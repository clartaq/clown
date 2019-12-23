;;;;
;;;; This namespace handles dragging the re-sizing border on the right-hand
;;;; side of the outline and notes areas. New sizes are sent to the server
;;;; to be persisted in the database.

(ns clown.client.util.dragging
  (:require [clojure.string :as s]
            [taoensso.timbre :refer [tracef debugf infof warnf errorf
                                     trace debug info warn error]]))

;;;-----------------------------------------------------------------------------
;;; Handle width dragging the splitters.

;; The minimum allowable width for the outline.
(def ^{:private true :const true} min-width-basis 250)

;; Could calculate this, but this is easier. This value must match that
;; used in the css file for the container.
(def ^{:private true :const true} twice-padding-width 0)

;; The resolved elements, just so we don't have to keep recalculating them.

(def ^{:private true} ele (atom nil))

;; Other dragger-related state.

(def ^{:private true} dragging (atom false))
(def ^{:private true} starting-mouse-x (atom 0))
(def ^{:private true} starting-basis (atom 0))
(def ^{:private true} new-basis (atom "0px"))
(def ^{:private true} preference-key (atom ""))
(def ^{:private true} prog-state-ratom (atom nil))

(defn persist-new-basis
  "Send the new basis back to the server to persist it."
  [new-basis]
  (debugf "persist-new-basis: new-basis: %s" new-basis)
  (let [num-basis (js/parseInt (s/replace-first new-basis "px" ""))]
    ((:send-message-fn @prog-state-ratom)
     (pr-str {:message {:command "hey-server/persist-new-preference-value"
                        :data    {:preference @preference-key
                                  :value      num-basis}}}))))

(defn- move [evt]
  (debugf "move: (.-pageX evt): %s" (.-pageX evt))
  (when @dragging
    (let [movement (- (.-pageX evt) @starting-mouse-x)]
      (reset! new-basis (str (max min-width-basis
                                  (+ @starting-basis movement)) "px"))
      (debugf "    @new-basis: %s" @new-basis)
      (set! (.-flexBasis (.-style @ele)) @new-basis))))

(defn- stop-tracking [_]
  (when @dragging
    (reset! dragging false)
    (.removeEventListener js/window "mousemove" move)
    (when (not= @starting-basis @new-basis)
      (persist-new-basis @new-basis))))

(defn- start-tracking [evt]
  (debugf "start-tracking: evt: %s, @starting-mouse-x: %s, (.-pageX evt): %s"
          evt @starting-mouse-x (.-pageX evt))
  (reset! starting-mouse-x (.-pageX evt)))

(defn ^{:export true} drag-click-handler [prog-state container-id pref-key]
  (debug "drag-click-handler")
  (reset! prog-state-ratom prog-state)
  (reset! preference-key pref-key)
  (reset! ele (.getElementById js/document container-id))
  (reset! starting-basis (- (.-offsetWidth @ele) twice-padding-width))
  (debugf "@starting-basis: %s" @starting-basis)
  (debugf "(.-offsetWidth @ele): %s" (.-offsetWidth @ele))
  (reset! dragging true)
  (.addEventListener js/window "mousedown" start-tracking)
  (.addEventListener js/window "mousemove" move)
  (.addEventListener js/window "mouseup" stop-tracking))
