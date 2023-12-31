(ns app.application
  (:require
   [ig.application :refer [start-application kill-app send-to-app!]]
   [com.stuartsierra.component :as component]
   [core.events :as e]
   [ig.rest :as rest]))

(defrecord Application [app-started auth-context]
  component/Lifecycle
  (start [this]
    (println "Starting Application")
    (if app-started
      this
      (let [{:keys [http-client]} auth-context
            file-name "logs/eventsource.log" #_(str "logs/" (.toString (java.time.LocalDateTime/now)) ".log")
            event-source-fn (fn [event] (spit file-name (str event "\n") :append true))
            order-executor (fn [{:keys [::e/data]}]
                             (let [{:keys [::e/name ::e/size ::e/direction]} data
                                   request (rest/open-order name direction size "EUR")]
                               (http-client request :error-callback (fn [status body] (send-to-app! (e/order-failure {::e/name name
                                                                                                                      ::e/status-code status
                                                                                                                      ::e/reason body
                                                                                                                      ::e/request request}))))))
            ok (start-application order-executor event-source-fn)]
        (-> this (assoc :app-started ok)))))
  (stop [this]
    (println "Stoping Application")
    (when app-started
      (kill-app)
      (assoc this :app-started nil))))

(defn make []
  (map->Application {}))
