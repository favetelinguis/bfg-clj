(ns app.stream
  (:require [clojure.core.async :as a]
            [app.auth-context :refer [auth-context]]
            [app.bfg :refer [bfg]]
            [mount.core :refer [defstate]]
            [bfg-ig.stream :as stream]))

(defn start-listener
  []
  (let [c (a/chan)
        callback (fn [event] (a/>!! c event))]
    (stream/create-connection-and-subscriptions! auth-context callback)
    (a/go-loop []
               (when-let [event (a/<! c)]
                 ;; TODO here we should update bfg based on event
                 (println event)
                 (recur)))
    c))


(defstate stream
          :start (start-listener)
          :stop (a/close! stream))
