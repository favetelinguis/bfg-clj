(ns bfg.indicators.time-series
  (:require [clojure.spec.alpha :as s]))

(s/def ::time inst?)

(defn time-series?
  [value-spec]
  (s/map-of ::time (s/and value-spec (s/keys :req-un [::time]))))

(defn num-periods
  [ts]
  (count ts))

(defn get-first
  [ts]
  (second (first ts)))

(defn get-second
  [ts]
  (second (second ts)))

(defn get-last
  [ts]
  (second (last ts)))

(defn make-empty
  "Make sure newest bar is first and oldest last"
  []
  (sorted-map-by (fn [k1 k2] (.compareTo k2 k1))))

(defn add
  "Check that each bar that is added in front is newer then the old bar
  This is very sensetive, if we update with a faulty value the barseries in a market will become nil and the
  trading system should stop  trading!
  TODO need more testing and thinking how to handle if data is not spaced even or not added in order?
  Order will be taken care of however spacing must be consitent.

  Bar must contain :id and :time
  "
  [ts & bars]
  (let [adder (fn [ts bar] (if-let [prior-bar (get-first ts)]
                             (let [is-same-market (= (:id bar) (:id prior-bar))
                                   is-new-bar-oldest (> (.compareTo (:time bar) (:time prior-bar)) 0)]
                               (when (and is-same-market is-new-bar-oldest)
                                 (assoc ts (:time bar) bar)))
                             (assoc ts (:time bar) bar)))]
    (reduce adder ts bars)))

(defn make-indicator-series
  "We need to work through the price seris in reverse order since time-series is from newest"
  [indicator-fn]
  (fn [time-series]
    (reduce
      (fn [agg bar] (add agg (indicator-fn agg bar)))
      (make-empty)
      (map second (reverse time-series)))))

(defn add-indicator-bar
  "Bars need to be from oldest to newest that is the reverse order then what the resulting timeseries will be"
  [indicator-fn]
  (fn
    [time-series & bars]
    (reduce
      (fn [agg bar] (add agg (indicator-fn agg bar)))
      time-series
      bars)))

(defn get-at
  [ts instant]
  (get ts instant))