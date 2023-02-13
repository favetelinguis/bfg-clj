(ns bfg.indicators.heikin-ashi-series-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.indicators.heikin-ashi-series :as ha]
    [bfg.data-test :as data]
    [bfg.indicators.ohlc-series :as ohlc]
    [bfg.indicators.time-series :as ts]
    )
  (:import (java.time Instant)))

(def ha-series (ha/make-heikin-ashi-series data/dax-bar-series-14))

(deftest calculate-heikin-ashi-test
  (is (= 14
         (ts/num-periods ha-series)))
  (is (= {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}
         (-> (ts/get-first (ha/make-heikin-ashi-series data/dax-bar-series-14))
             (select-keys [:o :h :l :c]))))
  (is (= 15
         (ts/num-periods (-> ha-series
                             (ha/add-heikin-ashi-bar data/bar-0))))))

(deftest direction-test
  (is (= :UP (ha/calculate-bar-direction (ts/get-first ha-series))))
  (is (= 0 (ha/num-consecutive-same-direction (ohlc/make-empty-series))))
  (is (= 1 (ha/num-consecutive-same-direction (-> (ohlc/make-empty-series)
                                                  (ohlc/add-ohlc-bar data/bar-3)
                                                  (ha/make-heikin-ashi-series)))))
  (is (= 1 (ha/num-consecutive-same-direction (-> (ohlc/make-empty-series)
                                                  (ohlc/add-ohlc-bar data/bar-2 data/bar-3)
                                                  (ha/make-heikin-ashi-series)))))
  (is (= 8 (ha/num-consecutive-same-direction ha-series))))

(deftest get-first-with-same-direction-test
  (is (= { :c 15034.9 :o 15029.368750000001 :h 15060.4 :l 15018.9}
         (select-keys (ha/get-first-with-same-direction ha-series) [:o :h :l :c]))))

(deftest is-entry-bar-test
  (is (not (ha/is-entry-bar? data/bar-2)))
  (is (true? (ha/is-entry-bar? (ohlc/make-bar :DAX (Instant/now)
                                              200.0 100.0 155.0 150.0)))))