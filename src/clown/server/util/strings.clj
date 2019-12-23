(ns clown.server.util.strings)

(defn remove-from-end
  "Remove any instance of 'end' from the end of string s
  and return the result."
  [s end]
  (if (.endsWith s end)
    (.substring s 0 (- (count s)
                       (count end)))
    s))

