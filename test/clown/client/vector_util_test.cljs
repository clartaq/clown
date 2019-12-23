;;;-----------------------------------------------------------------------------
;;; Tests for vector manipulation functions.

(ns ^:figwheel-always clown.client.vector-util-test
  (:require
    [clown.client.util.vector-utils :as vu]
    [cljs.test :refer-macros [deftest is testing]]))

(println "Loading clown.vector-util-test")

;-------------------------------------------------------------------------------
; Element deletion functions.

(deftest delete-at-test
  (testing "The 'delete-at' function"
    (is (= [] (vu/delete-at [3] 0)))
    (is (= ["a" "c"] (vu/delete-at ["a" "b" "c"] 1)))
    (is (thrown? js/Error (vu/delete-at ["a" "b" "c"] 5)))
    (is (thrown? js/Error (vu/delete-at nil 5)))
    (is (thrown? js/Error (vu/delete-at ["a" "b" "c"] -5)))
    (is (= ["b" "c"] (vu/delete-at ["a" "b" "c"] 0)))
    (is (= [\a \b] (vu/delete-at [\a\b\c] 2)))))

(deftest remove-first-test
  (testing "The 'remove-first' function"
    (is (= [2 3] (vu/remove-first [1 2 3])))
    (is (= [] (vu/remove-first [1])))
    (is (thrown? js/Error (vu/remove-first [])))
    (is (thrown? js/Error (vu/remove-first nil)))))

(deftest remove-last-test
  (testing "The 'remove-last' function"
    (is (= [] (vu/remove-last [0])))
    (is (= [1 2] (vu/remove-last [1 2 3])))
    (is (thrown? js/Error (vu/remove-last [])))
    (is (thrown? js/Error (vu/remove-last nil)))))

(deftest remove-last-two-test
  (testing "The 'remove-last-two' function"
    (is (= [1] (vu/remove-last-two [1 2 3])))
    (is (= [] (vu/remove-last-two [1 2])))
    (is (thrown? js/Error (vu/remove-last-two [1])))
    (is (thrown? js/Error (vu/remove-last-two [])))
    (is (thrown? js/Error (vu/remove-last-two nil)))))

;-------------------------------------------------------------------------------
; Insert, replace and append functions.

(deftest insert-at-test
  (testing "The 'insert-at' function"
    (is (= [1] (vu/insert-at [] 1 1)))
    (is (= ["a" 1] (vu/insert-at [1] 0 "a")))
    (is (= ["a" 1] (vu/insert-at [1] -5 "a")))
    (is (= [5] (vu/insert-at [] 4 5)))
    (is (= [7] (vu/insert-at [] 0 7)))
    ; NOTE: This might be unexpected
    (is (= '(5) (vu/insert-at nil 0 5)))
    (is (thrown? js/Error (vu/insert-at [1 2 3] nil 5)))
    ; NOTE: This might be unexpected
    (is (= [123 4 nil] (vu/insert-at [123 4] 4 nil)))))

(deftest prepend-test
  (testing "The 'prepend' test"
    (is (= [1] (vu/prepend []  1)))
    (is (= ["a" 1] (vu/prepend [1] "a")))
    (is (= ["a" 1] (vu/prepend [1] "a")))
    (is (= [5] (vu/prepend [] 5)))
    (is (= [7] (vu/prepend [] 7)))
    ; NOTE: This might be unexpected
    (is (= '(5) (vu/prepend nil 5)))
    ; NOTE: This might be unexpected
    (is (= [nil 123 4] (vu/prepend [123 4] nil)))))

(deftest replace-at-test
  (testing "The 'replace-at function"
    (is (thrown? js/Error (vu/replace-at [] 0 1)))
    (is (= [1] (vu/replace-at ["x"] 0 1)))
    (is (= [9 2 3 4 5] (vu/replace-at [1 2 3 4 5] 0 9)))
    (is (= ["a"] (vu/replace-at [1] 0 "a")))
    (is (thrown? js/Error (vu/replace-at [1] -5 "a")))
    (is (= [1 2 3 4 9 6 7] (vu/replace-at [1 2 3 4 5 6 7] 4 9)))
    (is (= [1 2 3 4 5 6 "Testing"] (vu/replace-at [1 2 3 4 5 6 7] 6 "Testing")))
    (is (= [7] (vu/replace-at [1] 0 7)))
    (is (thrown? js/Error (vu/replace-at nil 0 5)))
    (is (thrown? js/Error (vu/replace-at [1 2 3] nil 5)))
    (is (thrown? js/Error (vu/replace-at [123 4] 4 nil)))))

(deftest append-element-to-vector-test
  (testing "The 'append-element-to-vector' function")
  (is (= ["Funny"] (vu/append-element-to-vector [] "Funny")))
  (is (= [1 2 3] (vu/append-element-to-vector [1 2] 3)))
  ; NOTE: This might be unexpected.
  (is (= [3] (vu/append-element-to-vector nil 3)))
  ; NOTE: This might be unexpected.
  (is (= [1 2 nil] (vu/append-element-to-vector [1 2] nil))))

;-------------------------------------------------------------------------------
; Searching functions.

(deftest positions-test
  (testing "The 'positions' function"
    (is (= [] (vu/positions #{"q"} ["a" "b" "a" "c"])))
    (is (= [0 2] (vu/positions #{"a"} ["a" "b" "a" "c"])))
    (is (= [] (vu/positions #{72} [8 94 73 73 73 24 38])))
    (is (= [2 3 4] (vu/positions #{73} [8 94 73 73 73 24 38])))
    (is (= [] (vu/positions #{\q} [\b \c \w \w \w \h \k])))
    (is (= [2 3 4] (vu/positions #{\q} [\b \z \q \q \q \h \k])))))

(deftest find-and-delete-all-test
  (testing "The 'find-and-delete-all-all' function"
    (is (nil? (vu/find-and-delete-all nil "a")))
    (is (= ["a" "b"] (vu/find-and-delete-all ["a" "b"] nil)))
    (is (= [] (vu/find-and-delete-all ["a"] "a")))
    (is (= ["a"] (vu/find-and-delete-all ["a" "b"] "b")))
    (is (= ["a" "c"] (vu/find-and-delete-all ["a" "b" "c"] "b")))
    (is (= [] (vu/find-and-delete-all [1] 1)))
    (is (= [1] (vu/find-and-delete-all [1 2] 2)))
    (is (= [1 3] (vu/find-and-delete-all [1 2 3] 2)))
    (is (= [] (vu/find-and-delete-all [\a] \a)))
    (is (= [\a] (vu/find-and-delete-all [\a \b] \b)))
    (is (= [\a \c] (vu/find-and-delete-all [\a \b \c] \b)))))

(deftest prepend-uniquely-test
  (testing "The 'prepend-uniquely' function"
    (is (= ["a"] (vu/prepend-uniquely [] "a")))
    (is (= ["a" "c" "b"] (vu/prepend-uniquely ["c" "b" "a"] "a")))
    ))

