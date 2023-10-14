(ns bfg-ig.stream.subscription
(:require
 [bfg-ig.stream.item :as i]
   [bfg.market :as market]
   [meander.epsilon :as m]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [clojure.spec.alpha :as s]
   [bfg.error :as error])
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

(defn get-market-data-subscription
  "Return the market item subscription for epic"
  [subscriptions epic]
  (first
   (filter #(= (i/market-item epic)
               (first (seq (.getItems %)))) subscriptions)))
