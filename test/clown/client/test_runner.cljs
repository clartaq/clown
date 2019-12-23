;; This test runner is intended to be run from the command line
(ns clown.client.test-runner
  (:require
    ;; require all the namespaces that you want to test
    [clown.client.core-test]
    [clown.client.tree-id-test]
    [clown.client.undo-redo-test]
    [clown.client.vector-util-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))
