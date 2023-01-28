(ns bfg.market.indicators.atr
  (:require [bfg.market.bar :as bar]))

(defn calculate-tr
  [prior-bar current-bar]
  "If previous-bar is nil we return the first tr value"
  (if prior-bar
    (max
      (- (:h current-bar) (:l current-bar))
      (abs (- (:h current-bar) (:c prior-bar)))
      (abs (- (:l current-bar) (:c prior-bar))))
    (- (:h current-bar) (:l current-bar))
    )
  )

(defn make-atr-state
  "
  The length of the bar-series decides the number of periods to use for ATR
  "
  [bar-series]
  (println bar-series)
  (let [bars (bar/get-bars bar-series)
        initial-atr (/
                      (reduce + (map #(apply calculate-tr %) (partition 2 1 (conj bars nil))))
                      (bar/get-periods bar-series)
                      )]
    {::periods   (bar/get-periods bar-series)
     ::prior-atr initial-atr
     ::prior-bar (last bars)}))

(defn get-prior-bar
  [atrs]
  (get atrs ::prior-bar))

(defn get-prior-atr
  [atrs]
  (get atrs ::prior-atr))

(defn get-periods
  [atrs]
  (get atrs ::periods))

(defn update-prior-bar
  [atrs new-value]
  (assoc atrs ::prior-bar new-value))

(defn update-prior-atr
  [atrs new-value]
  (assoc atrs ::prior-atr new-value))

(defn update-atr
  "
  Current ATR = [(Prior ATR x 13) + Current TR] / 14
  Probably need about 14 + 14 bars to catch up with what is on IG
  "
  [atrs current-bar]
  (let [current-tr (calculate-tr (get-prior-bar atrs) current-bar)
        current-atr (/ (+ (* (get-prior-atr atrs) (- (get-periods atrs) 1)) current-tr) (get-periods atrs))]
    (-> atrs
        (update-prior-atr current-atr)
        (update-prior-bar current-bar))
    ))
