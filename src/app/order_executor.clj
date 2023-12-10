(ns app.order-executor
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]))

(defrecord OrderExecutor [channel order-store portfolio auth-context]
  ; TODO might not have to have auth-context here but i need to client to make call somewhere
  component/Lifecycle
  (start [this]
    (println "Starting OrderExecutor")
    (if channel
      this
      (let [o-c (:channel order-store)
            {:keys [mix]} portfolio
            c (a/chan 1)]
        (a/admix mix c)
        (a/go-loop []
          (when-let [x (a/<! o-c)]
            (println "Inside OrderExecutor loop")
            (a/go (a/>! c {:acc x})))
          (recur))
        (-> this
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping OrderExecutor")
    (when channel
      (assoc this :channel nil))))
