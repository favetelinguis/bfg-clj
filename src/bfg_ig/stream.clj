(ns bfg-ig.stream
  (:require
    [clojure.string :as str]
    )
  (:import (com.lightstreamer.client ClientListener LightstreamerClient Subscription SubscriptionListener)
           (java.util Arrays)))

(defn client-listener [callback]
  (reify
    ClientListener
    (onListenEnd [this a] (callback {:on-listen-end a}))
    (onListenStart [this a] (callback {:on-listen-start a}))
    (onServerError [this v1 v2] (callback {:on-server-error (str v1 v2)}))
    (onStatusChange [this status] (callback {:on-status-change status}))
    ))

(defn subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (callback {:on-subscription []}))
    (onListenStart [this subscription] (callback {:on-listen-start []}))
    (onListenEnd [this subscription] (callback {:on-listen-end []}))
    (onItemUpdate [this update] (callback {:on-item-update [(.getItemName update) (.getChangedFields update)]}))
    (onSubscriptionError [this code message] (callback {:on-subscription-error [code message]}))))

(defn trade-pattern
  [account-id]
  [(str "TRADE:" account-id)
   "MERGE" ])

(defn account-pattern
  [account-id]
  [(str "ACCOUNT:" account-id)
   "DISTINCT" ])

(defn chart-candle-pattern
  "Possible scale SECOND, 1MINUTE, 5MINUTE, HOUR"
  [scale epic]
  [(str/join ":" ["CHART" epic scale])
   "MERGE" ])

(def chart-candle-1min-pattern
  (partial chart-candle-pattern "1MINUTE"))

(defn create-subscription
  [market callback]
  (let [{:keys [epic]} market
        fields ["OFR_OPEN" "OFR_HIGH" "OFR_LOW" "OFR_CLOSE" "BID_OPEN" "BID_HIGH" "BID_LOW" "BID_CLOSE" "CONS_END" "UTM" "CONS_TICK_COUNT"]
        [items mode] (chart-candle-1min-pattern epic)
        subscription (Subscription.  mode (into-array String [items]) (into-array String fields))]
    (doto subscription
      (.addListener (subscription-listener callback))))
  )

(defn create-connection-and-subscriptions!
  [auth-context callback]
  (let [{:keys [identifier cst token ls-endpoint]} (deref auth-context)
        password (str "CST-" cst "|XST-" token)
        connection-listener (client-listener callback)
        client (LightstreamerClient. ls-endpoint nil)]
    (doto (.-connectionDetails client)
      (.setPassword password)
      (.setUser identifier))
    (doto client
      (.addListener connection-listener)
      (.connect)
      (.subscribe (create-subscription {:epic "IX.D.DAX.IFMM.IP"} callback))
      )))

(defn unsubscribe
  [client key])

(defn unsubscribe-all [lsclient subscriptions]
  (doseq [key subscriptions] (unsubscribe lsclient key)))