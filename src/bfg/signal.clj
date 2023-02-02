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

(s/def ::signal #{:await-entry :await-setup :await-exit})

(def consecutive-heikin-ashi-bars-trigger 10)
(def atr-multiple-setup-target 3)

(defn setup?
  [current-signal ohlc-series atr-series ha-series]
  (when (= current-signal :await-setup)
    (let [first-ha-bar (ha/get-first-with-same-direction ha-series)
          atr-at-first (:atr (ts/get-at atr-series (:time first-ha-bar)))
          target-price (+ (* atr-multiple-setup-target atr-at-first)
                          (:c first-ha-bar))
          is-over-target-price (>= (:h (ts/get-first ohlc-series)) target-price)
          is-fast-enough (<= (ha/num-consecutive-same-direction ha-series) consecutive-heikin-ashi-bars-trigger)
          ]
      (and is-over-target-price is-fast-enough))))

(defn entry?
  []
  )

(defn exit? [])
