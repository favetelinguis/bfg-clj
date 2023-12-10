(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [ig.stream.connection :as stream]))

(defrecord IgStream [connection channel topic config auth-context]
  component/Lifecycle
  (start [this]
    (if connection
      this
      (let [{:keys [data]} config
            {:keys [session]} auth-context
            conn (stream/create-connection data session)
            c (a/chan 1)
            topic (a/pub c :route)]
        (stream/connect! conn)
        (-> this
            (assoc :channel c)
            (assoc :topic topic)
            (assoc :connection conn)))))
  (stop [this]
    (if connection
      (do
        (stream/unsubscribe-all! connection)
        (stream/disconnect! connection)
        (a/close! channel)
        (assoc this :connection nil))
      this)))

(defn make []
  (map->IgStream {}))
