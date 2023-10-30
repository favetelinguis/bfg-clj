(ns core.indicators.ohlc-series
  (:require [core.indicators.time-series :as ts]
            [core.events :as e]))

(def make-empty-series ts/make-empty)

(def add-bar (ts/add-indicator-bar (fn [_ bar] bar)))

(defn down? [series]
  (let [a (ts/get-first series)
        b (ts/get-second series)]
    (when (and a b)
      (< (::e/close a) (::e/close b)))))

(defn up? [series]
  (let [a (ts/get-first series)
        b (ts/get-second series)]
    (when (and a b)
      (> (::e/close a) (::e/close b)))))
