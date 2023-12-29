(ns app.application
  (:require
   [ig.application :refer [start-application kill-app]]
   [com.stuartsierra.component :as component]
   [core.events :as e]))

(defrecord Application [app-started auth-context]
  component/Lifecycle
  (start [this]
    (println "Starting Application")
    (if app-started
      this
      (let [{:keys [http-client]} auth-context
            file-name "logs/eventsource.log" #_(str "logs/" (.toString (java.time.LocalDateTime/now)) ".log")
            event-source-fn (fn [event] (spit file-name (str event "\n") :append true))
            order-executor (fn [failure-fn order]
                             (failure-fn (::e/name order))) ; TODO this should use http-client and have a failure function if things fail
            ok (start-application order-executor event-source-fn)]
        (-> this (assoc :app-started ok)))))
  (stop [this]
    (println "Stoping Application")
    (when app-started
      (kill-app)
      (assoc this :app-started nil))))

(defn make []
  (map->Application {}))
