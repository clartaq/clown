;;;;
;;;; Utilities to help with determining what if particular fonts are
;;;; installed on the client system.
;;;;

(ns clown.client.util.font-detection
  (:require [clojure.string :as string]))

;; Filled in during loading of namespace.
(def measured-font-widths (atom {}))

(def generic-fonts [:monospace :serif :sans-serif])

;; Suggested by Lalit Patel
;; Website: http://www.lalit.org/lab/javascript-css-font-detect/ (broken)
(def test-string "mmmmmmmmmmlli")
(def test-size 72)

(def canvas (.createElement js/document "canvas"))
(def context (.getContext canvas "2d"))

(defn- measure-string-width
  "Return the width in pixels required to render the string in the given
  font at the given size."
  [s font-name font-size]
  (set! (.-font context) (str font-size "px " font-name))
  (.-width (.measureText context s)))

(defn- init-generic-font-widths!
  "Set the global variable containing the measured widths of the test
  string at the test size for each of the test fonts."
  []
  (mapv #(swap! measured-font-widths
                merge
                {% (measure-string-width test-string (name %) test-size)})
        generic-fonts))

(init-generic-font-widths!)

(defn- measure-against-generic
  "Measure the width of the test string using the font name and compare it
  to the width when one of the generic fonts (given by the generic-key) is
  used. Return true if the width does NOT match the width produced by the
  generic font. Return false otherwise."
  [font-name generic-key]
  (let [family (str test-size "px " font-name ", " (name generic-key))]
    (set! (.-font context) family)
    (let [width (measure-string-width test-string family test-size)]
      (not= width (generic-key @measured-font-widths)))))

(defn font-available?
  "Return true if the font named is available on the system."
  [font-name]
  (reduce #(or %1 (measure-against-generic font-name %2)) false generic-fonts))

;; Usage:
;;
;; (println "(font-available? \"Calibri\"): " (font-available? "Calibri"))
;; => true on Windows, false on Mac
; (println "(font-available? \"Calibri Regular\"): " (font-available? "Calibri Regular"))
;; => false
;; (println "(font-available? \"Arial\"): " (font-available? "Arial"))
;; => true
;; (println "(font-available? \"Boojum\"): " (font-available? "Boojum"))
;; => false
;; (println "(font-available? \"Helvetica Neue\"): " (font-available? "Helvetica Neue"))
;; => false on Windows, true on Mac
;; (println "(font-available? \"Helvetica Newish\"): " (font-available? "Helvetica Newish"))
;; => false

(defn font-family->font-used
  "Given a CSS font family, determine which is actually used. This is the
  first name in the list that is actually installed. Return nil if none of
  the fonts in the list are installed."
  [font-family]
  (let [names (mapv string/trim (string/split font-family #","))]
    (some #(when (font-available? %) %) names)))

;; (println "Selected from headline font family: " (font-family->font-used "\"Century Gothic\", Muli, \"Segoe UI\", Arial, sans-serif"))
;; => Muli ; on my system
;; (println "Selected from body font family: " (font-family->font-used "Palatino, \"Palatino Linotype\", \"Palatino LT STD\", \"Book Antiqua\", Georgia, serif"))
;; => Palatino ; on my system
;; (println "Selected from fixed font family: " (font-family->font-used "Consolas, \"Ubuntu Mono\", Menlo, Monaco, \"Lucida Console\",\n    \"Liberation Mono\", \"DejaVu Sans Mono\", \"Bitstream Vera Sans Mono\",\n    \"Courier New\", monospace, serif"))
;; => "Ubuntu Mono" ; on my system

