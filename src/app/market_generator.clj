(ns app.market-generator
  (:require
   [core.market.executor :as market-executor]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]))

(defrecord MarketGenerator [rx portfolio state]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (market-executor/start-market-generator portfolio state)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the market thread to exit
      (assoc this :rx nil))))

(defn new-market-generator []
  (map->MarketGenerator {:state (atom [])}))
