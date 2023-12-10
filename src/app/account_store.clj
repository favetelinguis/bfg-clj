(ns app.account-store
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [app.transducer-utils :as utils]))

(defn a-store-state-transducer [state]
  (let [f (fn [old event]
            (println "a-store-x")
            [[{:cand 3} {:pri 3}] (merge old event)])]
    (utils/make-state-transducer f state)))

(defrecord AccountStore [channel connection portfolio]
  component/Lifecycle
  (start [this]
    (println "Starting AccountStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            {:keys [mix]} portfolio
            c (a/chan 1 (a-store-state-transducer {}) utils/ex-fn)]
        (a/sub topic :account c)
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
