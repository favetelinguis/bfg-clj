(ns core.signal
  (:require [core.indicators.ohlc-series :as ohlc]
            [core.events :as e]))

(defprotocol Signal
  (get-name [this] "Get the name")
  (get-commands [this epic] "Return events from core.events to execute from last update")
  (on-update [this old change] "Should be [[event] old] change -> [[events] updated]"))

(defrecord DaxKillerSignal [num]
  Signal
  (get-name [this] "DAXKiller")

  (get-commands [this epic] (cond
                              (>= num 2) (e/create-new-order epic :buy)
                              (<= num -2) (e/create-new-order epic :sell)))
  (on-update [this [_ old] change] (cond
                                     (ohlc/down? change) (if (pos? num)
                                                           (assoc this :num -1)
                                                           (update this :num dec))
                                     (ohlc/up? change) (if (pos? num)
                                                         (update this :num inc)
                                                         (assoc this :num 1))
                                     :else (assoc this :num 0))))

(defn make-dax-killer-signal []
  (map->DaxKillerSignal {:num 0}))
