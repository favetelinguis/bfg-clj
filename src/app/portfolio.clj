(ns app.portfolio
  (:require
   [app.transducer-utils :as utils]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]))

(defn portfolio-state-transducer [state]
  ;; TODO import pure f for managing portfolio
  (let [f (fn [old event]
            (println "port-x")
            [[{:new 3} {:new 3}] (merge old event)])]
    (utils/make-state-transducer f state)))

(defrecord Portfolio [channel mix]
  component/Lifecycle
  (start [this]
    (println "Starting Port")
    (if channel
      this
      (let [c (a/chan 1 (portfolio-state-transducer {}) utils/ex-fn)
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
