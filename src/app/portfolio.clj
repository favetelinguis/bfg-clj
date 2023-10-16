(ns app.portfolio
  (:require
   [core.event.error :as error]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio.executor :as portfolio-executor]
   [core.portfolio.rules :as portfolio-rules]))

(defrecord Portfolio [rx session command-executor]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [command-executor-rx (:rx command-executor)
            session (atom (portfolio-rules/create-session
                           (fn [command] (a/>!! command-executor-rx command))))
            in-channel (portfolio-executor/start session)]
        (-> this
            (assoc :rx in-channel)
            (assoc :session session)))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new-portfolio []
  (map->Portfolio {}))
