(ns bfg.market.indicators.indicator-test
  (:require [clojure.test :refer :all])
  (:require [bfg.market.indicators.heikin-ashi :as ha]
            [bfg.data :as test-data]
            [bfg.market.indicators.atr :as atr]))

(deftest calculate-heikin-ashi-test
  (is (= #:bfg.indicators.heikin-ashi{:previous {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}}
         (reduce ha/calculate-heikin-ashi-bar nil test-data/dax-bar-series-14))))

(deftest atr-test
  (is (= #:bfg.indicators.atr{:periods   14,
                              :prior-atr 31.550531214683158,
                              :prior-bar {:h 15221.6, :l 15180.1, :o 15189.6, :c 15183.6}}
         (-> (atr/make-atr-state test-data/dax-bar-series-14)
             (atr/update-atr test-data/bar-0)
             (atr/update-atr test-data/bar-1)
             (atr/update-atr test-data/bar-2)
             (atr/update-atr test-data/bar-3)
             (atr/update-atr test-data/bar-4)
             (atr/update-atr test-data/bar-5)
             (atr/update-atr test-data/bar-6)
             (atr/update-atr test-data/bar-7)
             (atr/update-atr test-data/bar-8)
             ))))
