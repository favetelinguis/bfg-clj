(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [ig.stream.connection :as stream]
            [ig.market-cache :as market-cache]))

(defrecord IgStream [auth-context connection market-cache-state]
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

(defn new
  [auth-context]
  (map->IgStream {:auth-context auth-context
                  :market-cache-state (atom (market-cache/new))}))
