(ns bfg.indicators.heikin-ashi-series
  (:require [bfg.indicators.time-series :as ts]
            [clojure.spec.alpha :as s]))

(s/def ::id keyword?)
(s/def ::price (s/double-in :min 0. :max 100000.0))
(s/def ::o ::price)
(s/def ::h ::price)
(s/def ::l ::price)
(s/def ::c ::price)
(s/def ::time inst?)
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
      (calculate-current-ha-bar (ts/get-first heikin-ashi-series) current-price-bar)
      (calculate-first-ha-bar current-price-bar))))

(def make-heikin-ashi-series (ts/make-indicator-series calculate-heikin-ashi-bar))

(def add-heikin-ashi-bar
  "Takes heking-ashi series and a ohlc-bar and calculate the next hekin-ashi bar ans cons it to series"
  (ts/add-indicator-bar calculate-heikin-ashi-bar))

(def make-empty-series ts/make-empty)

(defn calculate-bar-direction
  [bar]
  (let [{:keys [o c]} bar]
    (cond
      (> o c) :DOWN
      (< o c) :UP
      :else :NEUTRAL)))

(defn same-direction?
  [bar1 bar2]
  (= (calculate-bar-direction bar1) (calculate-bar-direction bar2)))

(defn num-consecutive-same-direction
  "for empty ha-series return 0 else return 1 or more"
  [ha-series]
  (if (empty? ha-series)
    0
    (let [num-same (count
                     (take-while #(apply same-direction? %) (partition 2 1 (map second ha-series))))]
      (+ 1 num-same))
    ))

(defn get-first-with-same-direction
  [ha-series]
  (let [num-same (num-consecutive-same-direction ha-series)]
    (second (last (take num-same ha-series))))
  )


(def max-body-percentage 0.4)
(def max-wick-difference-percentage 0.1)

(defn is-entry-bar?
  "Want a bar with large wicks and a small body and the wicks are about the same size"
  [bar]
  (let [{:keys [o h l c]} bar
        total-range (abs (- h l))
        body-range (abs (- c o))
        [top-wick-range bottom-wick-range] (case (calculate-bar-direction bar)
                                             :UP [(- h c) (- o l)]
                                             :DOWN [(- h o) (- c l)]
                                             :NEUTRAL [(- h c) (- o l)])
        wick-range-difference (abs (- top-wick-range bottom-wick-range))
        total-wick-range (+ top-wick-range bottom-wick-range)]
    (and
      (not (zero? total-range))
      (not (zero? total-wick-range))
      (> max-body-percentage (/ body-range total-range))
      (> max-wick-difference-percentage (/ wick-range-difference total-wick-range))
      )))