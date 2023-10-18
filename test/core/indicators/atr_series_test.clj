(ns core.indicators.atr-series-test
  (:require [clojure.test :refer :all])
  (:require
    [core.data-test :as data]
    [core.indicators.atr-series :as atr]
    [bfg.indicators.time-series :as ts])
  )

(deftest atr-test
  (is (= 31.550531214683158
         (:atr
           (ts/get-first
             (-> (atr/make-atr-series data/dax-bar-series-14)
                 (atr/add-atr-bar data/bar-0 data/newest-dax-14)
                 (atr/add-atr-bar data/bar-1 data/bar-0)
                 (atr/add-atr-bar data/bar-2 data/bar-1)
                 (atr/add-atr-bar data/bar-3 data/bar-2)
                 (atr/add-atr-bar data/bar-4 data/bar-3)
                 (atr/add-atr-bar data/bar-5 data/bar-4)
                 (atr/add-atr-bar data/bar-6 data/bar-5)
                 (atr/add-atr-bar data/bar-7 data/bar-6)
                 (atr/add-atr-bar data/bar-8 data/bar-7)
                 ))))))
