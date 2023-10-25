(ns app.portfolio
  (:require
   [ig.command-executor :refer [->IgCommandExecutor]]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio :as portfolio-thread]
   [core.portfolio.rules :as rules]
   [core.command :as command]))

(defrecord Portfolio [rx rules-state auth-context]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [{:keys [http-client]} auth-context
            rs (atom (rules/create-session (command/->DummyCommandExecutor)))
            in-channel (portfolio-thread/start rs)]
        (-> this
            (assoc :rx in-channel)
            (assoc :rules-state rs)))))
  (stop [this]
    (when rx
      (a/close! rx))
    (assoc this :rx nil)))

(defn new []
  (map->Portfolio {}))
