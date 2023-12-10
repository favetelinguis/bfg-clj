(ns app.instrument-store
  (:require
   [com.stuartsierra.component :as component]
   [app.transducer-utils :as utils]
   [clojure.core.async :as a]
   [ig.market-cache :as market-cache]))

(defrecord InstrumentStore [channel market-topic stream]
  component/Lifecycle
  (start [this]
    (println "Starting InstrumentStore")
    (if channel
      this
      (let [{:keys [topic]} stream
            c (a/chan 1 (utils/make-state-transducer
                         market-cache/update-cache
                         (market-cache/make)) utils/ex-fn)
            m-topic (a/pub c :epic)]
        (a/sub topic "UNSUBSCRIBE" c)
        (a/sub topic "MARKET" c)
        (a/sub topic "CHART" c)
        (-> this
            (assoc :market-topic m-topic)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping Instrument")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defn make []
  (map->InstrumentStore {:state (atom {})}))

