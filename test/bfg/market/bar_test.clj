(ns bfg.market.bar-test
  (:require [bfg.market.bar :as sut])
  (:require
    [bfg.data-test :as test-data]
    [clojure.test :refer :all])
  (:import (java.time Instant)))

(deftest make-bar-test
  (let [now (Instant/now)]
    (is (=
          {:bfg.market.bar/id :DAX
           :c                 33
           :h                 111
           :l                 1
           :o                 22
           :time              now}
          (sut/make :DAX now 111 1 22 33)))))

(deftest make-bar-series-test
  (is (= #:bfg.market.bar{:bars ()
                          :id   :DAX}
         (sut/make-bar-series :DAX))))

(deftest add-bars-test
  (let [bars (-> (sut/make-bar-series :DAX)
                 (sut/add-bars
                   test-data/bar-0
                   test-data/bar-1
                   test-data/bar-2
                   ))]
    (is (= 3 (sut/get-periods bars)))))