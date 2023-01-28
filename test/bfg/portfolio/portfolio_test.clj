(ns bfg.portfolio.portfolio-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.portfolio.order :as order]
    [bfg.portfolio.position :as position]
    [bfg.portfolio.portfolio :as sut])
  (:import (java.time Instant)))

(def test-order
  (order/make :oid1 :DAX 33 :BUY 21.1))

(def test-position
  (position/make :pid1 33.4 (Instant/now)))

(deftest add-order-test
  (is (= 1 (count (sut/get-orders (-> (sut/make)
                                      (sut/add-order test-order)))))))

(deftest cancel-order-test
  (is (= 0 (count (sut/get-orders (-> (sut/make)
                                      (sut/add-order test-order)
                                      (sut/cancel-order {:order-id (::order/id test-order)})))))))

(deftest order->position-test
  (let [test-portfolio (-> (sut/make)
                           (sut/add-order test-order)
                           (sut/order->position {:order-id (::order/id test-order) :position test-position}))]
    (is (= 0 (count (sut/get-orders test-portfolio))))
    (is (= 1 (count (sut/get-positions test-portfolio))))))

(deftest order->position-order-id-missing-test
  (let [test-portfolio (-> (sut/make)
                           (sut/add-order test-order)
                           (sut/order->position {:order-id :missing :position test-position}))]
    (is (= nil test-portfolio))))

(deftest exit-position-test
  (let [test-portfolio (-> (sut/make)
                           (sut/add-order test-order)
                           (sut/order->position {:order-id (:id test-order) :position test-position})
                           (sut/exit-position (:id test-position)))]
    (is (= 0 (count (sut/get-orders test-portfolio))))
    (is (= 0 (count (sut/get-positions test-portfolio))))))
