(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [ig.stream.connection :as stream]
            [ig.market-cache :as market-cache]))

(defrecord IgStream [config auth-context connection market-cache-state]
  component/Lifecycle
  (start [this]
    (if connection
      this
      (let [{:keys [data]} config
            {:keys [session]} auth-context
            c (stream/create-connection data session)]
        (do
          (stream/connect! c)
          (assoc this :connection c)))))
  (stop [this]
    (if connection
      (do
        (stream/unsubscribe-all! connection)
        (stream/disconnect! connection)
        (assoc this :connection nil))
      this)))

(defn new []
  (map->IgStream {:market-cache-state (atom (market-cache/make))}))
