(ns core.indicators.ohlc-series-test
  (:require [clojure.test :refer :all]
            [core.events :as e]
            [core.indicators.time-series :as ts])
  (:require [core.indicators.ohlc-series :as ohlc])
  (:import (java.time Instant)))

(deftest add-candle-events-works
  (-> (ohlc/make-empty-series)
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      ((fn [xs]
         (is (= 1 (ts/num-periods xs)))
         xs))
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      ((fn [xs]
         (is (= 2 (ts/num-periods xs)))
         xs))))

(deftest down?-on-empty-series
  (-> (ohlc/make-empty-series)
      ((fn [xs]
         (is (not (ohlc/down? xs)))
         xs))))

(deftest down?-on-first-candle
  (-> (ohlc/make-empty-series)
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      ((fn [xs]
         (is (not (ohlc/down? xs)))
         xs))))

(deftest down?-on-equal-candle
  (-> (ohlc/make-empty-series)
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      ((fn [xs]
         (is (not (ohlc/down? xs)))
         xs))))

(deftest up?-series
  (-> (ohlc/make-empty-series)
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 100))
      ((fn [xs]
         (is (ohlc/up? xs))
         xs))))

(deftest down?-series
  (-> (ohlc/make-empty-series)
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 90))
      (ohlc/add-bar (e/create-candle-event "DAX" (Instant/now) 100 10 50 80))
      ((fn [xs]
         (is (ohlc/down? xs))
         xs))))
