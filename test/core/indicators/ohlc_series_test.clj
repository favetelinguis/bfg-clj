(ns core.indicators.ohlc-series-test
  (:require [clojure.test :refer :all])
  (:require [core.indicators.ohlc-series :as sut])
  (:import (java.time Instant)))

(deftest make-price-bar-test
  (let [now (Instant/now)]
    (is (=
         {:id :DAX
          :c                 33
          :h                 111
          :l                 1
          :o                 22
          :time              now}
         (sut/make-bar :DAX now 111 1 22 33)))))

