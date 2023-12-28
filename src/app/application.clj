(ns app.application
  (:require
   [ig.application :refer [start-app]]
   [com.stuartsierra.component :as component]
   [core.events :as e]))

(defrecord Application [make-strategy kill-app send-to-app!! auth-context]
  component/Lifecycle
  (start [this]
    (println "Starting Application")
    (if kill-app
      this
      (let [{:keys [http-client]} auth-context
            file-name "logs/eventsource.log" #_(str "logs/" (.toString (java.time.LocalDateTime/now)) ".log")
            event-source-fn (fn [event] (spit file-name (str event "\n") :append true))
            order-executor (fn [failure-fn order]
                             (failure-fn (::e/name order))) ; TODO this should use http-client and have a failure function if things fail
            {:keys [make-strategy kill-app send-to-app!!]} (start-app order-executor
                                                                      event-source-fn)]
        (-> this
            (assoc :make-strategy make-strategy)
            (assoc :kill-app kill-app)
            (assoc :send-to-app!! send-to-app!!)))))
  (stop [this]
    (println "Stoping Application")
    (when kill-app
      (kill-app)
      (assoc this :kill-app nil))))

(defn make []
  (map->Application {}))
