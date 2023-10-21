(ns app.portfolio
  (:require
   [ig.command-executor :refer [->IgCommandExecutor]]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio :as portfolio-thread]
   [core.portfolio.rules :as rules]))

(defrecord Portfolio [rx rules-state]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (portfolio-thread/start rules-state)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new [http-client]
  (map->Portfolio
   {:rules-state (atom (rules/create-session (->IgCommandExecutor http-client)))}))
