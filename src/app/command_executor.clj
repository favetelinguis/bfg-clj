(ns app.command-executor
  (:require
   [clojure.core.async :as a]
   [core.command.executor :as command-executor]
   [ig.command-executor :refer [->IgCommandExecutor]]
   [com.stuartsierra.component :as component]
   [core.event.error :as error]
   [core.events :as event]))

(defrecord CommandExecutor [rx command-executor-impl]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (command-executor/start command-executor-impl)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new [http-client]
  (map->CommandExecutor
   {:command-executor-impl (->IgCommandExecutor http-client)}))
