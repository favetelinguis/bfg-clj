(ns core.signal
  (:require [core.indicators.ohlc-series :as ohlc]
            [core.events :as e]))

(defprotocol Signal
  (get-name [this] "Get the name")
  (get-commands [this epic] "Return events from core.events to execute from last update")
  (on-midprice [this midprice] "Gets called on midprice")
  (on-candle [this candles] "Get called on candle"))

(defrecord DaxKillerSignal [num]
  Signal
  (get-name [this] "DAX Killer")

  (get-commands [this epic] (cond
                              (>= num 2) (e/create-new-order epic :buy)
                              (<= num -2) (e/create-new-order epic :sell)))

  (on-midprice [this midprice] this)

  (on-candle [this candles] (cond
                              (ohlc/down? candles) (if (pos? num)
                                                     (assoc this :num -1)
                                                     (update this :num dec))
                              (ohlc/up? candles) (if (pos? num)
                                                   (update this :num inc)
                                                   (assoc this :num 1))
                              :else (assoc this :num 0))))

(defn make-dax-killer-signal []
  (map->DaxKillerSignal {:num 0}))
