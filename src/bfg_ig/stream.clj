(ns bfg-ig.stream
  (:require
    [clojure.string :as str]
    )
  (:import (com.lightstreamer.client ClientListener LightstreamerClient Subscription SubscriptionListener)
           (java.util Arrays)))

; TODO put this in a component where state is in atom
; start will call connect!
; stop will call dissconnect!
; it will also take a out channel wrapt in a callback what will go to the MarketService in-channel
; but before we send it should be transformed to the connect event and be validated with meander!

(defn new-connection-state
  "TODO create a set of fn to update this."
  []
  {:subscriptions {}
   :subscription-error nil
   :connection-status "DISCONNECTED"})

(defn update-connection-status
  [connection-state status]
  (assoc connection-state :connection-status status))

(defn update subscription-error
  [connection-state error])

(defn client-listener [connection-state]
  (reify
    ClientListener
    (onListenEnd [this a] (callback {:on-listen-end a}))
    (onListenStart [this a] (callback {:on-listen-start a}))
    (onServerError [this v1 v2] (callback {:on-server-error (str v1 v2)}))
    (onStatusChange [this status] (update-connection-status connection-state status))
    ))

(defn subscription-listener [item-update-callback connection-state]
  (reify
    SubscriptionListener
    (onSubscription [this] (callback {:on-subscription []}))
    (onListenStart [this subscription] (add-subscription connection-state subscription))
    (onListenEnd [this subscription] (remove-subscription connection-state subscription))
    (onItemUpdate [this update] (item-update-callback (.getItemName update) (.getChangedFields update))) ;TODO item-update-callback should not have info of full update type, however different subscriptions have different fields
    (onSubscriptionError [this code message] (set-subscription-error connection-state code message))))

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
  [ callback connection-state]
  (let [fields ["OFR_OPEN" "OFR_HIGH" "OFR_LOW" "OFR_CLOSE" "BID_OPEN" "BID_HIGH" "BID_LOW" "BID_CLOSE" "CONS_END" "UTM" "CONS_TICK_COUNT"]
        [items mode] (chart-candle-1min-pattern epic)
        subscription (Subscription.  mode (into-array String [items]) (into-array String fields))]
    (doto subscription
      (.addListener (subscription-listener callback connection-state))))
  )

(defn create-connection
  [{:keys [identifier cst token ls-endpoint]} connection-state]
  (let [password (str "CST-" cst "|XST-" token)
        connection-listener (client-listener connection-state)
        client (LightstreamerClient. ls-endpoint nil)]
    (doto (.-connectionDetails client)
      (.setPassword password)
      (.setUser identifier))
    (doto client
      (.addListener connection-listener)
      )))

(defn connect! [connection]
  (.connect connection))

(defn dissconnect! [connection]
  (unsubscribe-all! connection)
  (.close connection))

(defn subscribe! [connection]
      (.subscribe connection subscription))

;; epic  "IX.D.DAX.IFMM.IP"

(defn unsubscribe!
  [client key])

(defn unsubscribe-all! [lsclient subscriptions]
  (doseq [key subscriptions] (unsubscribe lsclient key)))

