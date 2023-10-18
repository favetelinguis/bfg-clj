(ns app.portfolio
  (:require
   [ig.command-executor :refer [->IgCommandExecutor]]
   [core.event.error :as error]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio.executor :as portfolio-executor]
   [core.portfolio.rules :as portfolio-rules]))

(defrecord Portfolio [rx session]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (portfolio-executor/start session)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new [auth-context]
  (map->Portfolio
   (let [impl ]
     {:session (atom
                (portfolio-rules/create-session
                 (->IgCommandExecutor auth-context)))})))
