(ns bfg.system-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.market.bar :as bar]
    [bfg.market.market :as market]
    [bfg.market.signal :as signal]
    [bfg.portfolio.order :as order]
    [bfg.portfolio.portfolio :as portfolio]
    [bfg.portfolio.position :as position]
    [bfg.system :as sut]
    [bfg.account :as account]
    [bfg.data-test :as test-data])
  (:import (java.time Instant)))

(deftest add-market-test
  (is (map? (sut/get-market (-> test-data/test-system
                                (sut/add-market test-data/test-market)) :DAX)))
  )

(deftest update-account-test
  (is (= 200000 (account/get-total (sut/get-account
                                     (-> test-data/test-system
                                         (sut/update-account (account/make nil 200000 nil))))))))

(deftest update-portfolio-test
  (is (map? (portfolio/get-order
              (sut/get-portfolio
                (-> test-data/test-system
                    (sut/update-portfolio (merge
                                            {:type :order-created}
                                            (order/make :oid1 :DAX 2 :BUY 22.2))))) :oid1)))
  (is (nil? (portfolio/get-order
              (sut/get-portfolio
                (-> test-data/test-system
                    (sut/update-portfolio (merge
                                            {:type :order-created}
                                            (order/make :oid1 :DAX 2 :BUY 22.2)))
                    (sut/update-portfolio {:type :order-canceled :order-id :oid1}))) :oid1)))
  (is (nil? (portfolio/get-order
              (sut/get-portfolio
                (-> test-data/test-system
                    (sut/update-portfolio (merge
                                            {:type :order-created}
                                            (order/make :oid1 :DAX 2 :BUY 22.2)))
                    (sut/update-portfolio {:type     :position-opened
                                           :order-id :oid1
                                           :position (position/make :pid1 33.3 (Instant/now))}))) :oid1)))
  (is (nil? (portfolio/get-position
              (sut/get-portfolio
                (-> test-data/test-system
                    (sut/update-portfolio (merge
                                            {:type :order-created}
                                            (order/make :oid1 :DAX 2 :BUY 22.2)))
                    (sut/update-portfolio {:type     :position-opened
                                           :order-id :oid1
                                           :position (position/make :pid1 33.3 (Instant/now))})
                    (sut/update-portfolio {:type :position-exited :position-id :pid1}))) :pid1)))
  )

(deftest step-system-test
  (let [[system-1 orders-1] (-> test-data/test-system
                                (sut/add-market test-data/test-market)
                                (sut/step-system test-data/bar-0))
        [system-2 orders-2] (-> system-1
                                (sut/step-system test-data/bar-1))]

    (is (and
          (= 1 (signal/get-count-same-direction
                 (market/get-signal
                   (sut/get-market system-1 :DAX))))
          (= 2 (signal/get-count-same-direction
                 (market/get-signal
                   (sut/get-market system-2 :DAX))))))))