(ns bfg.market.indicators.heikin-ashi
  (:require [bfg.market.bar :as bar]
            [clojure.spec.alpha :as s]))

(s/def ::previous ::bar/bar)
(s/def ::state (s/keys :req [::previous]))

(defn- calculate-first-ha-bar
  [current-bar]
  {:c (/ (reduce + ((juxt :o :h :l :c) current-bar)) 4)
   :o (/ (+ (:o current-bar) (:c current-bar)) 2)
   :h (apply max ((juxt :o :h :c) current-bar))
   :l (apply max ((juxt :o :l :c) current-bar))
   })

(defn- calculate-current-ha-bar
  [previous-ha-bar current-bar]
  (let [hac (/ (reduce + ((juxt :o :h :l :c) current-bar)) 4)
        hao (/ (+ (:o previous-ha-bar) (:c previous-ha-bar)) 2)
        hah (max (:h current-bar) hao hac)
        hal (min (:l current-bar) hao hac)]
    {:c hac
     :o hao
     :h hah
     :l hal
     }))

(defn get-previous
  [heikin-ashi-state]
  (get heikin-ashi-state ::previous))

(defn calculate-heikin-ashi-bar
  "
  If previous-ha-bar is nil calculate the initial HA bar else calculate next based on previous
  The effect of the initial ha-bar usually goes away after 7-10 bars so allow some warmup period
  "
  [previous-ha-bar current-bar]
  (if previous-ha-bar
    (calculate-current-ha-bar previous-ha-bar current-bar)
    (calculate-first-ha-bar current-bar)))

(defn step-heikin-ashi-state
  [heikin-ashi-state current-bar]
  (assoc heikin-ashi-state ::previous
                           (calculate-heikin-ashi-bar (get-previous heikin-ashi-state) current-bar)))

(defn make-heikin-ashi-state
  "note that bar series is always ordered from newest to oldest so we need to reverse the order
  when calculating historic values"
  [bar-series]
  {::previous (reduce calculate-heikin-ashi-bar nil (reverse (bar/get-bars bar-series)))}
  )

(defn calculate-bar-direction
  "Can brob be moved if i have a bar-series namespace"
  [bar]
  (let [{:keys [o c]} bar]
    (cond
      (> o c) :DOWN
      (< o c) :UP
      :else :NEUTRAL)))
