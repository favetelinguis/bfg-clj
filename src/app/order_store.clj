(ns app.order-store
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [ig.order-cache :as order-cache]
            [app.transducer-utils :as utils]))

(defrecord OrderStore [channel stream portfolio]
  component/Lifecycle
  (start [this]
    (println "Starting OrderStore")
    (if channel
      this
      (let [{:keys [topic]} stream
            port-c (:channel portfolio)
            c (a/chan 1 (utils/make-state-transducer
                         order-cache/update-cache
                         (order-cache/make)) utils/ex-fn)
            mix (a/mix c)]
        (a/admix mix port-c)
        (a/sub topic "TRADE" c)
        (-> this
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping OrderStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defn make []
  (map->OrderStore {}))
