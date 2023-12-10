(ns app.order-store
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [app.transducer-utils :as utils]))

(defn o-store-state-transducer [state]
  (let [f (fn [old event]
            (println "o-store-x")
            (if (:create-o event) ; to break infinit lops only creae-o should be passed on to oexecutor
              [[{:new 3} {:new 3}] (merge old event)]
              [[] (merge old event)]))]
    (utils/make-state-transducer f state)))

(defrecord OrderStore [channel connection port]
  component/Lifecycle
  (start [this]
    (println "Starting OrderStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            port-c (:channel port)
            c (a/chan 1 (o-store-state-transducer {}) utils/ex-fn)
            mix (a/mix c)]
        (a/admix mix port-c)
        (a/sub topic :trade c)
        (-> this
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping OrderStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))
