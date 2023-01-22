(ns bfg.portfolio-test
  (:require [clojure.test :refer :all])
  (:require [bfg.portfolio :as sut])
  (:import (java.time Instant)))

(def test-order
  (sut/make-order :asdf :fdsa :DAX 33 :BUY 21.1 23.8))

(def test-position
  (sut/make-position :poss 33.4 (Instant/now)))

(deftest add-order-test
  (is (= 1 (count (:orders (-> (sut/make-portfolio)
                               (sut/add-order test-order)))))))

(deftest cancel-order-test
  (is (= 0 (count (:orders (-> (sut/make-portfolio)
                               (sut/add-order test-order)
                               (sut/cancel-order (:id test-order))))))))

(deftest order->position-test
  (let [test-portfolio (-> (sut/make-portfolio)
                           (sut/add-order test-order)
                           (sut/order->position (:id test-order) test-position))]
    (is (= 0 (count (:orders test-portfolio))))
    (is (= 1 (count (:positions test-portfolio))))))

(deftest order->position-order-id-missing-test
  (let [test-portfolio (-> (sut/make-portfolio)
                           (sut/add-order test-order)
                           (sut/order->position :missing test-position))]
    (is (= nil test-portfolio))))

(deftest exit-position-test
  (let [test-portfolio (-> (sut/make-portfolio)
                           (sut/add-order test-order)
                           (sut/order->position (:id test-order) test-position)
                           (sut/exit-position (:id test-position)))]
    (is (= 0 (count (:orders test-portfolio))))
    (is (= 0 (count (:positions test-portfolio))))))
