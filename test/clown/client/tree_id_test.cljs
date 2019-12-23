;;;-----------------------------------------------------------------------------
;;; Tests for tree id handling functions.

(ns ^:figwheel-always clown.client.tree-id-test
  (:require [clown.client.tree-ids :as ti]
            [cljs.test :refer-macros [deftest is testing]]))

(defonce ts (ti/topic-separator))

(deftest tree-id->tree-id-parts-test
  (testing "The 'tree-id->tree-id-parts' function")
  (is (= ["root" "0" "topic"] (ti/tree-id->tree-id-parts
                                (str "root" ts 0 ts "topic"))))
  (is (nil? (ti/tree-id->tree-id-parts nil)))
  (is (nil? (ti/tree-id->tree-id-parts ""))))

(deftest tree-id-parts->tree-id-string-test
  (testing "The 'tree-id-parts->tree-id-string-test"
    (is (= (str "root" ts 0 ts "topic") (ti/tree-id-parts->tree-id-string
                                          ["root" 0 "topic"])))
    (is (= (str "root" ts 0 ts "topic") (ti/tree-id-parts->tree-id-string
                                          ["root" "0" "topic"])))
    (is (nil? (ti/tree-id-parts->tree-id-string nil)))
    (is (nil? (ti/tree-id-parts->tree-id-string {})))))

(deftest roundtrip-test
  (testing "That the above two functions produce a correct round trip of the data"
    (is (= ["root" "0" "topic"] (ti/tree-id->tree-id-parts
                                  (ti/tree-id-parts->tree-id-string
                                    ["root" "0" "topic"]))))
    (is (= ["root" "0" "topic"]
           (ti/tree-id->tree-id-parts
             (ti/tree-id-parts->tree-id-string ["root" "0" "topic"]))))))

(deftest top-tree-id-test
  (testing "The 'top-tree-id' function"
    (is (= (ti/tree-id-parts->tree-id-string["root" "0" "topic"])
           (ti/top-tree-id)))
    (is (= ["root" "0" "topic"] (ti/tree-id->tree-id-parts (ti/top-tree-id))))))

(deftest is-top-tree-id?-test
  (testing "The 'top-tree-id?' function"
    (is (true? (ti/top-tree-id? (str "root" ts "0" ts "topic"))))
    (is (true? (ti/top-tree-id? (str "root" ts "0" ts "anything"))))
    (is (false? (ti/top-tree-id? "root-0-topic")))))

(deftest tree-id->nav-index-vector-test
  (testing "The 'tree-id->nav-index-vector' function"
    (is (= ["0"] (ti/tree-id->nav-index-vector (str "root" ts 0 ts "topic"))))
    (is (= ["0"] (ti/tree-id->nav-index-vector (str "root" ts "0" ts "topic"))))
    (is (= ["0"] (ti/tree-id->nav-index-vector
                   (str "root" ts 0 ts "anything"))))
    (is (= ["0"] (ti/tree-id->nav-index-vector
                   (str "root" ts "0" ts "anything"))))
    (is (= ["0" "5" "32"] (ti/tree-id->nav-index-vector
                            (str "root" ts 0 ts 5 ts 32 ts "topic"))))
    (is (= ["0" "5" "32"] (ti/tree-id->nav-index-vector
                            (str "root" ts "0" ts "5" ts "32" ts "topic"))))))

(deftest parent-id-test
  (testing "The `tree-id->parent-id` function."
    (is (nil? (ti/tree-id->parent-id (str "root" ts 0 ts "topic"))))
    (is (nil? (ti/tree-id->parent-id (str "root" ts 35 ts "topic"))))
    (is (= (str "root" ts 35 ts "topic") (ti/tree-id->parent-id (str "root" ts 35 ts 0 ts "topic"))))
    (is (= (str "root" ts 35 ts "topic") (ti/tree-id->parent-id (str "root" ts 35 ts 35 ts "topic"))))
    ))

(deftest nav-index-vector->tree-id-string-test
  (testing "The 'nav-index-vector->tree-id' function"
    (is (= (str "root" ts 0 ts "topic")
           (ti/nav-index-vector->tree-id-string [0])))
    (is (= (str "root" ts 0 ts "topic")
           (ti/nav-index-vector->tree-id-string ["0"])))
    (is (= (str "root" ts 0 ts "anything")
           (ti/nav-index-vector->tree-id-string [0] "anything")))
    (is (= (str "root" ts 0 ts "anything")
           (ti/nav-index-vector->tree-id-string ["0"] "anything")))
    (is (= (str "root" ts 0 ts 5 ts 32 ts "topic")
           (ti/nav-index-vector->tree-id-string [0 5 32])))
    (is (= (str "root" ts 0 ts 5 ts 32 ts "topic")
           (ti/nav-index-vector->tree-id-string ["0" "5" "32"])))))

(deftest tree-id->sortable-nav-string-test
  (testing "The 'tree-id->sortable-nav-string' function"
    (is (= "0" (ti/tree-id->sortable-nav-string
                 (str "root" ts "0" ts "topic"))))
    (is (= "2-5-32" (ti/tree-id->sortable-nav-string
                      (str "root" ts "2" ts "5" ts "32" ts "anything"))))))

(deftest set-leaf-index-test
  (testing "The 'set-leaf-index-test' function"
    (is (= (str "root" ts "5" ts "topic")
           (ti/set-leaf-index (str "root" ts "0" ts "topic") 5)))
    (is (= (str "root" ts "2" ts "5" ts "14" ts "anything")
           (ti/set-leaf-index
             (str "root" ts "2" ts "5" ts "32" ts "anything") 14)))))

(deftest increment-leaf-index-by-test
  (testing "The 'increment-leaf-index-by' function"
    (is (= (str "root" ts "1" ts "topic")
           (ti/increment-leaf-index-by (str "root" ts "0" ts "topic") 1)))
    (is (= (str "root" ts "2" ts "5" ts "35" ts "anything")
           (ti/increment-leaf-index-by
             (str "root" ts "2" ts "5" ts "32" ts "anything") 3)))
    (is (= (str "root" ts "2" ts "5" ts "29" ts "anything")
           (ti/increment-leaf-index-by
             (str "root" ts "2" ts "5" ts "32" ts "anything") -3)))))

(deftest increment-leaf-index-test
  (testing "The 'increment-leaf-index' function"
    (is (= (str "root" ts "1" ts "topic")
           (ti/increment-leaf-index (str "root" ts "0" ts "topic"))))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "anything")
           (ti/increment-leaf-index
             (str "root" ts "2" ts "5" ts "32" ts "anything"))))))

(deftest decrement-leaf-index-test
  (testing "The 'decrement-leaf-index' function"
    (is (= (str "root" ts "-1" ts "topic")
           (ti/decrement-leaf-index (str "root" ts "0" ts "topic"))))
    (is (= (str "root" ts "2" ts "5" ts "31" ts "anything")
           (ti/decrement-leaf-index
             (str "root" ts "2" ts "5" ts "32" ts "anything"))))))

(deftest change-tree-id-type-test
  (testing "The 'change-tree-id-type' function"
    (is (= (str "root" ts 0 ts "topic")
           (ti/change-tree-id-type (str "root" ts 0 ts "anything") "topic")))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "franjooly")
           (ti/change-tree-id-type
             (str "root" ts "2" ts "5" ts "33" ts "anything") "franjooly")))))

(deftest insert-child-index-into-parent-id-test
  (testing "The insert-child-index-into-tree-id->parent-id' function"
    ; Child index can be integer or string.
    (is (= (str "root" ts "2" ts "5" ts "33" ts "topic")
           (ti/insert-child-index-into-parent-id
             (str "root" ts "2" ts "5" ts "anything") 33)))
    (is (= (str "root" ts "2" ts "5" ts "33" ts "topic")
           (ti/insert-child-index-into-parent-id
             (str "root" ts "2" ts "5" ts "anything") "33")))))

(deftest tree-id->tree-path-nav-vector-test
  (testing "The 'tree-id->tree-path-nav-vector' function"
    (is (= [0] (ti/tree-id->tree-path-nav-vector
                 (str "root" ts 0 ts "topic"))))
    (is (= [2 :children 5 :children 33]
           (ti/tree-id->tree-path-nav-vector
             (str "root" ts "2" ts "5" ts "33" ts "anything"))))))

(deftest tree-id->nav-vector-and-index-test
  (testing "The 'tree-id->nav-vector-and-index' function"
    (let [nvi {:path-to-parent []
               :child-index    0}]
      (is (= nvi (ti/tree-id->nav-vector-and-index
                   (str "root" ts 0 ts "topic")))))
    (let [nvi {:path-to-parent [2 :children 5]
               :child-index    33}]
      (is (= nvi (ti/tree-id->nav-vector-and-index
                   (str "root" ts "2" ts "5" ts "33" ts "anything")))))))