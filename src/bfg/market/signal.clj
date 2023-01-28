(ns bfg.market.signal
  (:require [bfg.market.indicators.atr :as atr]
            [bfg.market.indicators.heikin-ashi :as ha]))

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

(defn make
  "
  Takes the initial bar-series, observe that the length of this bar-series decides the number of periods for ATR
  This means that no trading will start until enough bars has passed as required by the strategy
  "
  [{:keys [bar-series atr-multiple-setup-target consecutive-heikin-ashi-bars-trigger]}]
  {
   ::state                                :await-setup
   ::consecutive-heikin-ashi-bars-trigger consecutive-heikin-ashi-bars-trigger
   ::atr-multiple-setup-target            atr-multiple-setup-target
   ::atr-state                            (atr/make-atr-state bar-series)
   ::heikin-ashi-state                    (ha/make-heikin-ashi-state bar-series)
   ::strategy-state                       {
                                           ::count-same-direction 0
                                           ::first-atr            nil ;; atr at first bar
                                           ::first                nil ;; when count = 1
                                           ::last                 nil ;; previous-bar when entry we have entry bar
                                           ::entry                nil ;; first bar in opposite direction
                                           }
   })

(defn get-consecutive-heikin-ashi-bars-trigger
  [s]
  (get s ::consecutive-heikin-ashi-bars-trigger))

(defn get-first
  [s]
  (get-in s [::strategy-state ::first]))

(defn get-last
  [s]
  (get-in s [::strategy-state ::last]))

(defn get-entry
  [s]
  (get-in s [::strategy-state ::entry]))

(defn get-atr-multiple-setup-target
  [s]
  (get-in s [::atr-multiple-setup-target]))

(defn get-first-atr
  [s]
  (get-in s [::strategy-state ::first-atr]))

(defn get-target-price
  [s]
  (+ (* (get-atr-multiple-setup-target s)
        (get-first-atr s))
     (:c (get-first s))))

(defn get-heikin-ashi-state
  [s]
  (get-in s [::heikin-ashi-state]))

(defn get-count-same-direction
  [s]
  (get-in s [::strategy-state ::count-same-direction]))

(defn update-heikin-ashi-state
  [s new-val]
  (assoc-in s [::heikin-ashi-state] new-val))

(defn get-state
  [s]
  (get s ::state))

(defn update-state
  [s new-val]
  (assoc s ::state new-val))

(defn get-atr-state
  [s]
  (get s ::atr-state))

(defn update-atr-state
  [s new-val]
  (assoc s ::atr-state new-val))

(defn update-count-same-direction
  [s new-val]
  (assoc-in s [::strategy-state ::count-same-direction] new-val))

(defn update-first
  [s new-val]
  (assoc-in s [::strategy-state ::first] new-val))

(defn update-last
  [s new-val]
  (assoc-in s [::strategy-state ::last] new-val))

(defn update-entry
  [s new-val]
  (assoc-in s [::strategy-state ::entry] new-val))

(defn update-first-atr
  [s new-val]
  (assoc-in s [::strategy-state ::first-atr] new-val))

(defmulti step-signal (fn [s _] (::state s)))

(defmethod step-signal :await-setup [s current-bar]
  (let [previous-atr (atr/get-prior-atr (get-atr-state s))
        current-atr-state (atr/update-atr (get-atr-state s) current-bar)
        previous-ha-state (get-heikin-ashi-state s)
        current-ha-state (ha/step-heikin-ashi-state previous-ha-state current-bar)
        new-count (if (= (ha/calculate-bar-direction
                           (ha/get-previous previous-ha-state)) (ha/calculate-bar-direction (ha/get-previous current-ha-state)))
                    (inc (get-count-same-direction s))
                    1)
        test-setup-rules #(and (>= (:h current-bar) (get-target-price %))
                               (<= (get-count-same-direction %) (get-consecutive-heikin-ashi-bars-trigger %)))]
    (as-> s $
          (update-count-same-direction $ new-count)
          (update-heikin-ashi-state $ current-ha-state)
          (update-atr-state $ current-atr-state)
          (update-first $ (if (= new-count 1)
                            (ha/get-previous previous-ha-state)
                            (get-first $)))
          (update-first-atr $ (if (= new-count 1)
                                previous-atr
                                (get-first-atr $)))
          (update-state $ (if (test-setup-rules $)
                            :await-entry
                            :await-setup))
          )))

(defmethod step-signal :await-entry [s current-bar]
  (let [previous-ha-state (get-heikin-ashi-state s)
        current-ha-state (ha/step-heikin-ashi-state previous-ha-state current-bar)
        current-atr-state (atr/update-atr (get-atr-state s) current-bar)
        new-count (if (= (ha/calculate-bar-direction (ha/get-previous previous-ha-state)) (ha/calculate-bar-direction (ha/get-previous current-ha-state)))
                    (inc (get-count-same-direction s))
                    1)
        test-entry-rules #(= new-count 1)]
    (as-> s $
          (update-count-same-direction $ new-count)
          (update-heikin-ashi-state $ current-ha-state)
          (update-atr-state $ current-atr-state)
          (update-last $ (if (test-entry-rules)
                           (ha/get-previous previous-ha-state)
                           nil))
          (update-entry $ (if (test-entry-rules)
                            (ha/get-previous current-ha-state)
                            nil))
          (update-state $ (if (test-entry-rules)
                            :await-exit
                            :await-entry)))
    ))

(defmethod step-signal :await-exit [s current-bar]
  (let [previous-ha-state (get-heikin-ashi-state s)
        current-ha-state (ha/step-heikin-ashi-state previous-ha-state current-bar)
        current-atr-state (atr/update-atr (get-atr-state s) current-bar)
        new-count (if (= (ha/calculate-bar-direction (ha/get-previous previous-ha-state)) (ha/calculate-bar-direction (ha/get-previous current-ha-state)))
                    (inc (get-count-same-direction s))
                    1)
        test-exit-rules #(= new-count 1)]
    (as-> s $
          (update-count-same-direction $ new-count)
          (update-heikin-ashi-state $ current-ha-state)
          (update-atr-state $ current-atr-state)
          (update-state $ (if (test-exit-rules)
                            :await-setup
                            :await-exit)))
    )
  )