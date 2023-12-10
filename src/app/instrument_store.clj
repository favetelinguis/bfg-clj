(ns app.instrument-store
  (:require
   [com.stuartsierra.component :as component]
   [app.transducer-utils :as utils]
   [clojure.core.async :as a]))

(defn m-store-state-transducer [state]
  (let [f (fn [old event]
            (println "m-store-x")
            [[{:cand 3} {:pri 3}] (merge old event)])]
    (utils/make-state-transducer f state)))

(defrecord InstrumentStore [channel market-topic connection]
  component/Lifecycle
  (start [this]
    (println "Starting InstrumentStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            c (a/chan 1 (m-store-state-transducer {}) utils/ex-fn)
            m-topic (a/pub c :epic)]
        (a/sub topic :market c)
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

