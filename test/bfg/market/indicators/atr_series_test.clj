(ns bfg.market.indicators.atr-series-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.market.indicators.heikin-ashi-series :as ha]
    [bfg.data-test :as data]
    [bfg.market.indicators.atr-series :as atr])
  (:import (java.time.temporal ChronoUnit)))

(deftest calculate-heikin-ashi-test
  (is (= {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}
         (ha/get-previous (ha/make-heikin-ashi-series data/dax-bar-series-14))
         )))

(deftest atr-test
  (is (= #:bfg.market.indicators.atr-series{:periods 14,
                                     :prior-atr      31.550531214683158}
         (-> (atr/make-atr-state data/dax-bar-series-14)
             (atr/update-atr data/bar-0)
             (atr/update-atr data/bar-1)
             (atr/update-atr data/bar-2)
             (atr/update-atr data/bar-3)
             (atr/update-atr data/bar-4)
             (atr/update-atr data/bar-5)
             (atr/update-atr data/bar-6)
             (atr/update-atr data/bar-7)
             (atr/update-atr data/bar-8)
             (select-keys [::atr/periods ::atr/prior-atr])  ;hard to test prior bar due to time
             ))))


