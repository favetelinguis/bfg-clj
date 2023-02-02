(ns bfg.indicators.time-series-test
  (:require [bfg.indicators.time-series :as ts]
            [bfg.data-test :as data])
  (:require [clojure.test :refer :all])
  (:import (java.time Instant)))

(deftest get-at-test
  (let [ok-time (Instant/parse "2022-01-25T09:03:00Z")
        nok-time (Instant/parse "2002-01-25T09:03:00Z")]
    (is (= {:o 14998.2 :h 15038.7 :l 14964.2 :c 15028.7 :id :DAX :time ok-time}
           (ts/get-at data/dax-bar-series-14 ok-time)))
    (is (nil? (ts/get-at data/dax-bar-series-14 nok-time)))))
