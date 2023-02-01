(ns bfg.market.indicators.heikin-ashi-series-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.market.indicators.heikin-ashi-series :as ha]
    [bfg.data-test :as data]
    [bfg.market.indicators.time-series :as ts]
    ))

(deftest calculate-heikin-ashi-test
  (is (= 14
         (ts/num-periods (ha/make-heikin-ashi-series data/dax-bar-series-14))))
  (is (= {:c 15142.575 :h 15161.5 :l 15127.369287109375 :o 15127.369287109375}
         (-> (ts/newest (ha/make-heikin-ashi-series data/dax-bar-series-14))
             (select-keys [:o :h :l :c]))))
  (is (= 15
         (ts/num-periods (-> (ha/make-heikin-ashi-series data/dax-bar-series-14)
                             (ha/add-heikin-ashi-bar data/bar-0))))))
