(ns bfg.signal-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.data-test :as data]
    [bfg.indicators.ohlc-series :as ohlc]
    [bfg.indicators.heikin-ashi-series :as ha]
    [bfg.indicators.atr-series :as atr]
    [bfg.signal :as signal])
  (:import (java.time Instant)))

(def setup-trigger-bar
  (ohlc/make-bar :DAX (Instant/parse "2022-01-25T09:19:00Z") 15855. 15215. 15222. 15777.))

(def ha-series
  (-> (ha/make-empty-series)
      (ha/add-heikin-ashi-bar data/bar-1)
      (ha/add-heikin-ashi-bar data/bar-2)
      (ha/add-heikin-ashi-bar setup-trigger-bar)
      ))

(def ohlc-series
  (-> (ohlc/make-empty-series)
      (ohlc/add-ohlc-bar data/bar-0)
      (ohlc/add-ohlc-bar data/bar-1)
      (ohlc/add-ohlc-bar data/bar-2)
      (ohlc/add-ohlc-bar setup-trigger-bar)
      ))

(def atr-series
  (with-redefs [atr/periods 2]
    (-> (atr/make-atr-series (-> (ohlc/make-empty-series) (ohlc/add-ohlc-bar data/bar-0) (ohlc/add-ohlc-bar data/bar-1)))
        (atr/add-atr-bar data/bar-2 data/bar-1)
        (atr/add-atr-bar setup-trigger-bar data/bar-2)
        )))

(deftest setup-test
  (with-redefs [atr/periods 2
                signal/atr-multiple-setup-target 1
                signal/consecutive-heikin-ashi-bars-trigger 2]
    (is (signal/setup? :await-setup ohlc-series atr-series ha-series))
    (is (not (signal/setup? :await-entry ohlc-series atr-series ha-series)))
    ))