(ns app.stream
  (:require [com.stuartsierra.component :as component]
            [ig.application :refer [send-to-app!!]]
            [ig.stream.connection :as stream]
            [ig.stream.subscription :as subscription]))

(defrecord IgStream [connection config auth-context application]
  component/Lifecycle
  (start [this]
    (if connection
      this
      (let [{:keys [data]} config
            {:keys [session]} auth-context
            account-id (:currentAccountId session)
            account-sub (subscription/new-account-subscription account-id send-to-app!!)
            trade-sub (subscription/new-trade-subscription account-id send-to-app!!)
            conn (stream/create-connection data session)]
        (stream/connect! conn)
        (stream/subscribe! conn account-sub trade-sub) ; start trade and subscription
        (-> this
            (assoc :connection conn)))))
  (stop [this]
    (if connection
      (do
        (stream/unsubscribe-all! connection)
        (stream/disconnect! connection)
        (assoc this :connection nil))
      this)))

(defn make []
  (map->IgStream {}))
