(ns bfg.market.indicators.indicator-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.market.bar :as bar]
    [bfg.market.indicators.heikin-ashi :as ha]
    [bfg.data-test :as data]
    [bfg.market.indicators.atr :as atr]))

(deftest calculate-heikin-ashi-test
  (is (= {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}
         (ha/get-previous (ha/make-heikin-ashi-state data/dax-bar-series-14))
         )))

(deftest calculate-heikin-ashi-useing-reductions-test
  (first (reverse (drop 1 (reductions ha/calculate-heikin-ashi-bar nil (reverse (bar/get-bars data/dax-bar-series-14)))))) (is (= {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}
         )))

(deftest atr-test
  (is (= #:bfg.market.indicators.atr{:periods   14,
                              :prior-atr 31.550531214683158}
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