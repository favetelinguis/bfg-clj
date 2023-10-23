(ns app.portfolio
  (:require
   [ig.command-executor :refer [->IgCommandExecutor]]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio :as portfolio-thread]
   [core.portfolio.rules :as rules]))

(defrecord Portfolio [rx rules-state auth-context]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [{:keys [http-client]} auth-context
            rs (atom (rules/create-session (->IgCommandExecutor http-client)))
            in-channel (portfolio-thread/start rules-state)]
        (-> this
         (assoc :rx in-channel)
         (assoc :rules-state rs)))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new []
  (map->Portfolio {}))
