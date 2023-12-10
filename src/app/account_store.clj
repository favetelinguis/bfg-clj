(ns app.account-store
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [ig.account-cache :as account-cache]
            [app.transducer-utils :as utils]))

;; Currently there is no way to change account or remove account, we only suupport singe account
(defrecord AccountStore [channel stream portfolio]
  component/Lifecycle
  (start [this]
    (println "Starting AccountStore")
    (if channel
      this
      (let [{:keys [topic]} stream
            {:keys [mix]} portfolio
            c (a/chan 1 (utils/make-state-transducer
                         account-cache/update-cache
                         (account-cache/make)) utils/ex-fn)]
        (a/sub topic "ACCOUNT" c)
        (a/admix mix c)
        (-> this
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping AccountStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defn make []
  (map->AccountStore {}))
