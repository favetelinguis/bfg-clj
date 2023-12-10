(ns app.portfolio
  (:require
   [app.transducer-utils :as utils]
   [ig.portfolio :as portfolio]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]))

(defrecord Portfolio [channel mix]
  component/Lifecycle
  (start [this]
    (println "Starting Port")
    (if channel
      this
      (let [c (a/chan 1 (utils/make-state-transducer
                         portfolio/update-cache
                         (portfolio/make)) utils/ex-fn)
            port-mix (a/mix c)]
        (-> this
            (assoc :mix port-mix)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping Port")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defn make []
  (map->Portfolio {}))
