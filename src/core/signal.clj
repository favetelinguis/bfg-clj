(ns core.signal
  (:require [core.indicators.ohlc-series :as ohlc]
            [core.events :as e]
            [ig.cache :as cache]))

(defprotocol Signal
  (get-name [this] "Get the name")
  (on-update [this old change] "Should be [[event] old] change -> [[events] updated]"))

(defrecord DaxKillerSignal [num]
  ;; TODO signal need to change, cant have get-commands
  Signal
  (get-name [this] "DAXKiller")

  (on-update [this [_ old] change] (do
                                     (println "In strategy" old change)
                                     (cache/make)) #_(cond
                                                       (ohlc/down? change) (if (pos? num)
                                                                             (assoc this :num -1)
                                                                             (update this :num dec))
                                                       (ohlc/up? change) (if (pos? num)
                                                                           (update this :num inc)
                                                                           (assoc this :num 1))
                                                       :else (assoc this :num 0))))

(defn make-dax-killer-signal []
  (map->DaxKillerSignal {:num 0}))

(defn dax-killer [[_ old] {:keys [::e/data]}]
  (let [{:keys [::e/name]} data
        {:keys [bars]} old
        ;; TODO should only do this on new bars not each mid-price change
        next (update old :bars inc)
        event (if (zero? (mod bars 2))
                [(e/signal {::e/name name ::e/direction :sell})]
                [(e/signal {::e/name name ::e/direction :buy})])]
    (cache/make event next)))
