(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [ig.stream.connection :as stream]
            [ig.market-cache :as market-cache]
            [ig.rest :as rest]))

(defrecord IgStream [auth-context http-client connection market-cache-state]
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
            (stream/unsubscribe-all! connection)
            (http-client (rest/logout))
            (assoc this :connection nil))
          this)))

(defn new
  [auth-context http-client]
  (map->IgStream {:auth-context auth-context
                  :http-client http-client
                  :market-cache-state (atom (market-cache/new))}))
