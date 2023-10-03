; Only one connection is allowed, max 40 subscriptions is allowed on that connection.
(ns bfg-ig.stream
  (:require
   [bfg.market :as market]
   [meander.epsilon :as m]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]

   [clojure.spec.alpha :as s])
  (:import (com.lightstreamer.client ClientListener LightstreamerClient Subscription SubscriptionListener)
           (java.util Arrays)))

; TODO
; Add subscription given market - have a way to create all types of subscriptions
; Remove subscription given market
; Check java example how to puzzle together objects
; This component depends on all different market/account/potrfolio in channels user either pub/sub multimethod or some cond to route messages to the correct channel
;
(def max-subscriptions 40)

(defn new-connection-state
  "TODO create a set of fn to update this."
  []
  {:subscriptions {}
   :subscription-error nil})

(defn update-connection-status
  [connection-state status]
  (assoc connection-state :connection-status status))

(defn update-subscription-error
  [connection-state error])

(defn add-subscription
  [connection-state subscription])

(defn remove-subscription
  [connection-state subscription])

(defn client-listener [connection-state]
  (reify
    ClientListener
    (onListenEnd [this a] (println "onListenEnd" a))
    (onListenStart [this a] (println "onListenStart" a))
    (onServerError [this v1 v2] (println "onServerError" v1 v2))
    (onStatusChange [this status] (update-connection-status connection-state status))
    ))

(defn new-subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (println "onSubscription"))
    (onListenStart [this subscription] (println subscription))
    (onListenEnd [this subscription] (println subscription))
    (onItemUpdate [this update] (callback update))
    (onSubscriptionError [this code message] (println (str code ": " message)))))

(defn create-subscription
  [item mode fields callback]
  (let [subscription (Subscription.  mode (into-array String [item]) (into-array String fields))]
    (doto subscription
      (.addListener (new-subscription-listener callback)))))

(defn item-update->bfg-market-update-event!
  "Ensure that incomming data conforms to event structure.
  Throws exceptions need to catch Throwable both :post and m/match throws exceptions
  and are not catched in this function
  {:type :market-update,
  :epic IX.D.DAX.IFMM.IP,
  :update-time 17:29:51, TODO convert to date
  :market-delay 0, TODO convert to boolean
  :market-state TRADEABLE,
  :bid 15109.8, TODO convert to double
  :offer 15112.6} TODO convert to double
  "
  ;; TODO maybe we need an error type to use or how should i handle exeptions?
  [item-update]
  {:post [(s/valid? :market/event %)]}
  (let [epic (second (str/split (.getItemName item-update) #":"))
        changed-fields (into {} (.getChangedFields item-update)) ; return an immutable java map so convert it to someting Clojure
        ]
    (m/match changed-fields

      {"UPDATE_TIME" ?UPDATE_TIME
       "MARKET_DELAY" ?MARKET_DELAY
       "MARKET_STATE" ?MARKET_STATE
       "BID" ?BID
       "OFFER" ?OFFER}

      {:market/update-time ?UPDATE_TIME
       :market/market-delay ?MARKET_DELAY
       :market/market-state ?MARKET_STATE
       :market/bid ?BID ; we can delete this and only use candle for price
       :market/offer ?OFFER ; we can delete this and only use candle for price
       :market/type :market/market-update
       :market/epic epic})))

(defn new-market-subscription
  "TODO how to get channel in for each callback fn"
  [epic]
  (let [item (str "MARKET" ":" epic)
        mode "MERGE"
        fields ["UPDATE_TIME" "MARKET_DELAY" "MARKET_STATE" "BID" "OFFER"]
        ;; TODO how to catch exceptions that happen in the callback function?
        callback (fn [item-update] (println (item-update->bfg-market-update-event item-update)))]
    (create-subscription item mode fields callback)))

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

(defn get-subscriptions [connection]
  (.getSubscriptions connection))

(defn connect! [connection]
  (.connect connection))

(defn subscribe! [connection subscription]
  (when (> max-subscriptions (count (get-subscriptions connection)))
    (.subscribe connection subscription)))


(defn unsubscribe!
  [connection subscription]
  (.unsubscribe connection subscription))

(defn unsubscribe-all! [lsclient subscriptions]
  (doseq [key subscriptions] (unsubscribe! lsclient key)))

(defn disconnect! [connection]
  ;; (unsubscribe-all! connection)
  (.disconnect connection))

(defn get-status [connection]
  (.getStatus connection))

(defn get-subscription-item
  [connection subscription]
  (-> connection
      .getSubscriptions
      first
      .getItems
      seq)) ; use seq to get from Sring[] to someting more clojure


(defrecord IgStream [auth-context connection connection-state]
  component/Lifecycle
  (start [this]
         (if connection
           this
           (let [c (create-connection auth-context connection-state)]
             (connect! c)
             (assoc this :connection c))))
  (stop [this]
        (if connection
          (do
            (disconnect! connection)
            (dissoc this :connection))
          this)))

(defn new-stream
  [auth-context]
  (map->IgStream {
                  :auth-context auth-context
                  :connection-state (atom (new-connection-state))
                  }))

(defn create-system
  [auth-context]
  (let []
    (component/system-map
                           :stream (new-stream auth-context)
                           )))

(comment
  (require 'config 'bfg-ig.setup)
  (def config (config/load!))
  (def auth-context (bfg-ig.setup/create-session! config))
  (def state (atom (new-connection-state)))
  (def conn (create-connection auth-context state))
  ;; (def system (create-system auth-context))
  ;; (alter-var-root #'system component/start)
  ;; (alter-var-root #'system component/stop)
  (def msub (new-market-subscription "IX.D.DAX.IFMM.IP"))
  (subscribe! conn msub)
  (connect! conn)
  (disconnect! conn)
  (get-subscriptions conn)
  (get-status conn)
  (get-subscriptions conn)
  (.unsubscribe conn (first (get-subscriptions conn)))
  )

