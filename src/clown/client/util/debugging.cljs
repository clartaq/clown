;;;
;;; clown.client.util.debugging This namespace may be expanded with functions
;;; that are useful for debugging, but not directly used in the main
;;; functionality of the program.
;;;

(ns clown.client.util.debugging)

;; Flat
(defn js-obj->clj-map
  "Convert the properties of a javascript object into a clojure/script map.
  Only looks one level deep."
  [obj]
  (-> (fn [result key]
        (let [v (goog.object/get obj key)]
          (if (= "function" (goog/typeOf v))
            result
            (assoc result key v))))
      (reduce {} (.getKeys goog/object obj))))

;; Recursive
(defn js-obj->clj-map-recursively
  "Convert the properties of a javascript object into a clojure/script map.
  If a property is itself an object, convert the nested object as well."
  [obj]
  (if (goog.isObject obj)
    (-> (fn [result key]
          (let [v (goog.object/get obj key)]
            (if (= "function" (goog/typeOf v))
              result
              (assoc result key (js-obj->clj-map-recursively v)))))
        (reduce {} (.getKeys goog/object obj)))
    obj))