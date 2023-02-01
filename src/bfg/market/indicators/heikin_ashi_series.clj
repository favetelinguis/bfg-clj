(ns bfg.market.indicators.heikin-ashi-series
  (:require [bfg.market.indicators.time-series :as ts]
            [clojure.spec.alpha :as s]))

(s/def ::id keyword?)
(s/def ::price (s/double-in :min 0. :max 100000.0))
(s/def ::o ::price)
(s/def ::h ::price)
(s/def ::l ::price)
(s/def ::c ::price)
(s/def ::bar (s/keys :req-un [::id ::time ::o ::h ::l ::c]))
(s/def ::series (ts/time-series? ::bar))

(defn calculate-first-ha-bar
  [current-bar]
  {:c (/ (reduce + ((juxt :o :h :l :c) current-bar)) 4)
   :o (/ (+ (:o current-bar) (:c current-bar)) 2)
   :h (apply max ((juxt :o :h :c) current-bar))
   :l (apply max ((juxt :o :l :c) current-bar))
   })

(defn calculate-current-ha-bar
  [previous-ha-bar current-price-bar]
  (let [hac (/ (reduce + ((juxt :o :h :l :c) current-price-bar)) 4)
        hao (/ (+ (:o previous-ha-bar) (:c previous-ha-bar)) 2)
        hah (max (:h current-price-bar) hao hac)
        hal (min (:l current-price-bar) hao hac)]
    {:c hac
     :o hao
     :h hah
     :l hal
     }))

(defn calculate-heikin-ashi-bar
  "
  If previous-ha-bar is nil calculate the initial HA bar else calculate next based on previous
  The effect of the initial ha-bar usually goes away after 7-10 bars so allow some warmup period
  "
  [heikin-ashi-series current-price-bar]
  (merge
    (select-keys current-price-bar [:id :time])                   ; add id and time from original bar
    (if-not (empty? heikin-ashi-series)
      (calculate-current-ha-bar (ts/newest heikin-ashi-series) current-price-bar)
      (calculate-first-ha-bar current-price-bar))))

(def make-heikin-ashi-series (ts/make-indicator-series calculate-heikin-ashi-bar))

(def add-heikin-ashi-bar (ts/add-indicator-bar calculate-heikin-ashi-bar))

(defn calculate-bar-direction
  [bar]
  (let [{:keys [o c]} bar]
    (cond
      (> o c) :DOWN
      (< o c) :UP
      :else :NEUTRAL)))
