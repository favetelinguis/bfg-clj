(ns core.indicators.atr-series
  (:require [core.indicators.time-series :as ts]
            [clojure.spec.alpha :as s]))

(s/def ::atr (s/double-in :min 0. :max 100000.0))
(s/def ::bar (s/keys :req-un [::id ::time ::atr]))
(s/def ::series (ts/time-series? ::bar))

(def periods 14)

(defn after-atr-init?
  [ohlc-series]
  (> (count ohlc-series) periods))

(defn init-atr?
  [ohlc-series]
  (= periods (count ohlc-series)))

(defn calculate-tr
  [prior-bar current-bar]
  "If previous-bar is nil we return the first tr value"
  (if prior-bar
    (max
      (- (:h current-bar) (:l current-bar))
      (abs (- (:h current-bar) (:c prior-bar)))
      (abs (- (:l current-bar) (:c prior-bar))))
    (- (:h current-bar) (:l current-bar))))

(defn make-atr-series
  "
  OBS Length of ohlc-serice must match periods for atr
  "
  [ohlc-series]
  (when (= periods (count ohlc-series))
    (let [bars (map second (reverse ohlc-series))
          newest-bar (select-keys (ts/get-first ohlc-series) [:time :id])
          initial-atr (/
                        (reduce + (map #(apply calculate-tr %) (partition 2 1 (cons nil bars))))
                        periods
                        )
          atr-bar (merge newest-bar {:atr initial-atr})]
      (ts/add (ts/make-empty) atr-bar))))

(defn calculate-atr
  "
  Current ATR = [(Prior ATR x 13) + Current TR] / 14
  Probably need about 14 + 14 bars to catch up with what is on IG
  "
  [atr-series current-bar prior-bar]
  (let [current-tr (calculate-tr prior-bar current-bar)
        atr (/ (+ (* (:atr (ts/get-first atr-series)) (- periods 1)) current-tr) periods)]
    (merge
      (select-keys current-bar [:time :id])
      {:atr atr})
    ))

(defn add-atr-bar
  [atr-series current-ohlc-bar prior-ohlc-bar]
  (-> atr-series
      (ts/add (calculate-atr atr-series current-ohlc-bar prior-ohlc-bar))))
