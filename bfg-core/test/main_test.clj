(ns main-test
  (:require [clojure.test :refer [deftest is testing]]
            [main :as sut]))

(deftest oneandone-test
  (testing "Test that 1 + 1 is 2"
    (is (= (sut/oneandone) 2)))
  (testing "Is not more then 2"
    (is (not (> (sut/oneandone) 2)))))