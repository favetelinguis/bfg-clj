(ns bfg.core-test
  (:require [clojure.test :refer :all])
  (:require [bfg.core :refer [adder]]))

(deftest adder-test
  (testing "Adding adds"
    (is (= 2 (adder))))
  )
