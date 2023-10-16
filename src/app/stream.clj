(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [ig.stream.connection :as stream]
            [ig.stream.subscription :as subscription]))

(defrecord IgStream [auth-context connection]
  component/Lifecycle
  (start [this]
         (if connection
           this
           (let [c (stream/create-connection auth-context)]
             (stream/connect! c)
             (assoc this :connection c))))
  (stop [this]
        (if connection
          (do
            (stream/disconnect! connection)
            (assoc this :connection nil))
          this)))

(defn new-stream
  [auth-context]
  (map->IgStream {:auth-context auth-context}))
