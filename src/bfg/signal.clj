(ns bfg.signal
  (:require [bfg.indicators.atr-series :as atr]
            [bfg.indicators.heikin-ashi-series :as ha]
            [bfg.indicators.time-series :as ts]
            [clojure.spec.alpha :as s]))

(comment "
WO = Working Order - In market
OCO = One Cancels Other - Stop-Loss and Target

Strategy based on heikin-ashi reversals.
I believe its possible to find oversold or overbought areas using reversal patterns in heikin-ashi chars.

Setup: Check if x consecutive heikin-ashi moves +-(y*(ATR at first heikin-ashi in current direction)) within z bars.
       Then wait for first bar to turn in different direction.
Entry: Place a working order at low/high of entry bar. Target=2xATR Stop-Loss=Opposite direction of entry bar OR min for market
Close Working Order: If no position is open within 5 bars
Exit Position: If order not stop-loss hit, or any new bar is formed that points in opposite direction then my wanted.
")

(s/def ::signal #{:await-entry :await-setup :await-wo-confirmation :await-exit})

(def consecutive-heikin-ashi-bars-trigger 10)
(def atr-multiple-setup-target 3)

(defn setup?
  [ohlc-series atr-series ha-series]
  (when (and (seq ohlc-series) (seq atr-series) (seq ha-series))
    (let [first-ha-bar (ha/get-first-with-same-direction ha-series)
          atr-at-first (:atr (ts/get-at atr-series (:time first-ha-bar)))
          target-price (+ (* atr-multiple-setup-target atr-at-first)
                          (:c first-ha-bar))
          is-over-target-price (>= (:h (ts/get-first ohlc-series)) target-price)
          is-fast-enough (<= (ha/num-consecutive-same-direction ha-series) consecutive-heikin-ashi-bars-trigger)
          ]
      (and is-over-target-price is-fast-enough))))

(defn entry?
  [ha-series]
  (let [bar1 (ts/get-first ha-series)
        bar2 (ts/get-second ha-series)]
    (and
      (not (ha/same-direction? bar1 bar2))
      ;; TODO should also check that entry bar do not span to much of action
      (ha/is-entry-bar? bar1)))
  )

(defn cancel-entry?
  [ha-series]
  (let [bar1 (ts/get-first ha-series)
        bar2 (ts/get-second ha-series)]
    (or
      (not (ha/same-direction? bar1 bar2))
      ;; TODO should also check that entry bar do not span to much of action
      (ha/is-entry-bar? bar1)))
  )

(defn wo-confirmation?
  "Check that the order is correct"
  [order]
  true)

(defn close-wo? [order ha-series]
  )

(defn exit-position? [order ha-series]
  )

