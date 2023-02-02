(ns bfg.indicators.ohlc-series
  (:require [clojure.spec.alpha :as s]
            [bfg.indicators.time-series :as ts]
            ))

(s/def ::id keyword?)
(s/def ::price (s/double-in :min 0. :max 100000.0))
(s/def ::o ::price)
(s/def ::h ::price)
(s/def ::l ::price)
(s/def ::c ::price)
(s/def ::bar (s/keys :req-un [::id ::time ::o ::h ::l ::c]))
(s/def ::series (ts/time-series? ::bar))

(defn make-bar [market-id time high low open close]
  {:id market-id :time time :o open :h high :l low :c close})

(defn market-id [bar]
  (::id bar))

(def make-empty-series ts/make-empty)

(def add-ohlc-bar (ts/add-indicator-bar (fn [_ bar] bar)))

