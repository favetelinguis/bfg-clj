(ns app.application
  (:require
   [ig.application :refer [start-app]]
   [com.stuartsierra.component :as component]))

(defrecord Application [make-strategy kill-app send-to-app!! auth-context]
  component/Lifecycle
  (start [this]
    (println "Starting Application")
    (if kill-app
      this
      (let [{:keys [http-client]} auth-context
            file-name (str "logs/" (.toString (java.time.LocalDateTime/now)) ".log")
            debug-fn (fn [event] (spit file-name (str event "\n") :append true))
            {:keys [make-strategy kill-app send-to-app!!]} (start-app http-client
                                                                      debug-fn)]
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
