(ns ig.stream.subscription
(:require
   [ig.stream.item :as i]
   [meander.epsilon :as m]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [clojure.spec.alpha :as s])
(:import (com.lightstreamer.client ClientListener LightstreamerClient Subscription SubscriptionListener)
           (java.util Arrays))
  )

(defn- new-subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (println "onSubscription"))
    (onListenStart [this subscription] (println subscription))
    (onListenEnd [this subscription] (println subscription))
    (onItemUpdate [this update] (callback update))
    (onSubscriptionError [this code message] (println (str code ": " message)))))

(defn- create-subscription
  [item mode fields callback]
  (let [subscription (Subscription.  mode (into-array String [item]) (into-array String fields))]
    (doto subscription
      (.addListener (new-subscription-listener callback)))))

(defn new-market-subscription
  "tx-fn should be a fn that takes a :market/event"
  [epic tx-fn]
  (let [item (i/market-item epic)
        mode "MERGE"
        ; TODO remove bid offer and use candle stream as only source to reduce load
        fields ["UPDATE_TIME" "MARKET_DELAY" "MARKET_STATE" "BID" "OFFER"]
        callback #(-> % i/market-item-update->bfg-market-update-event tx-fn)]
    (create-subscription item mode fields callback)))

(defn new-account-subscription
  [{:keys [account]} tx-fn]
  (let [item (i/account-item account)
        mode "MERGE"
        fields ["AVAILABLE_CASH" "FUNDS" "MARGIN"]
        callback #(-> i/market-item-update->bfg-account-update-event tx-fn)]
    (create-subscription item mode fields callback)))

(defn new-trade-subscription
  [{:keys [account]} tx-fn]
  (let [item (i/trade-item account)
        mode "DISTINCT"
        fields ["CONFIRMS" "OPU" "WOU"]
        callback #(-> i/market-item-update->bfg-trade-update-event tx-fn)]
    (create-subscription item mode fields callback)))

(defn new-trade-subscription
  [{:keys [account]} tx-fn])

(defn get-item
  "Will return item used for subscription, for example MARKET:<epic>
  Is represented as String[] seq is used to get Clojure representation"
  [subscription]
  (first (seq (.getItems subscription))))

(defn get-market-data-subscription
  "Return the market item subscription for epic"
  [subscriptions epic]
  (first
   (filter #(= (i/market-item epic) (get-item %)) subscriptions)))

(defn get-subscribed-epics
  "Will return seq of MARKET subscriptions "
  [subscriptions]
  (map (comp i/get-epic get-item) subscriptions))
