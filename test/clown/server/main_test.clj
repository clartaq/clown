(ns clown.server.main-test
  (:require [clojure.test :refer :all]))

(println "clown.main-test")

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))

(deftest b-test
  (testing "This one should work."
    (is (= \b (first "boffo")))))

