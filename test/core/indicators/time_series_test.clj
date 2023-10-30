(ns core.indicators.time-series-test
  (:require [core.indicators.time-series :as ts]
            [data-test :as data]
            [core.events :as e])
  (:require [clojure.test :refer :all])
  (:import (java.time Instant)))

(deftest get-at-test
  (let [ok-time (Instant/parse "2022-01-25T09:03:00Z")
        nok-time (Instant/parse "2002-01-25T09:03:00Z")]
    (is (let [{:keys [::e/open ::e/high ::e/low ::e/close ::e/name ::e/time]}
              (ts/get-at data/dax-bar-series-14 (Instant/parse "2022-01-25T09:03:00Z"))]
          (and (= open 14998.2)
               (= high 15038.7)
               (= low 14964.2)
               (= close 15028.7)
               (= name "DAX")
               (= time ok-time))))
    (is (nil? (ts/get-at data/dax-bar-series-14 nok-time)))))
