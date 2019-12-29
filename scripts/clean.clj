(require '[clojure.java.io :as io])

;; A list of directory names that are to be deleted, including all child
;; files and directories. The directory names are relative to the project
;; directory.
(def clean-targets ["target"
                    "test/data/db"
                    "resources/public/db"
                    "resources/public/main.out"
                    "resources/public/main.js"
                    "resources/public/main-auto-testing.js"
                    "resources/public/test-main.out"
                    "resources/public/test-main.js"])

;; Stolen from prod.clj in the full-stack-clj-example
;; https://github.com/oakes/full-stack-clj-example
(defn delete-children-recursively! [f]
      (when (.isDirectory f)
            (doseq [f2 (.listFiles f)]
                   (delete-children-recursively! f2)))
      (when (.exists f) (io/delete-file f)))

(defn delete-clean-targets! [v]
  (doseq [dir-name v]
    (let [dir-file (io/file dir-name)]
      (delete-children-recursively! dir-file))))

(delete-clean-targets! clean-targets)
(System/exit 0)