(ns ig.stream.subscription
  (:require
   [ig.stream.item :as i]
   [ig.market-cache :as market-cache]
   [clojure.string :as str])
  (:import (com.lightstreamer.client Subscription SubscriptionListener)))

(defn- new-subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (println "onSubscription"))
    (onListenStart [this subscription] (println subscription))
    (onListenEnd [this subscription] (println subscription))
    (onItemUpdate [this item-update] (callback item-update))
    (onSubscriptionError [this code message] (println (str code ": " message)))))

(defn- new-subscription
  [item mode fields callback]
  (let [subscription (Subscription.  mode (into-array String [item]) (into-array String fields))]
    (doto subscription
      (.addListener (new-subscription-listener callback)))))

(defn new-market-subscription
  "state is an atom used to build up market state, f is connection to portfolio and should
  takes a vararg of events"
  [epic f market-cache-state]
  (let [item (i/market-item epic)
        mode "MERGE"
        ; TODO remove bid offer and use candle stream as only source to reduce load
        fields ["UPDATE_TIME" "MARKET_DELAY" "MARKET_STATE" "BID" "OFFER"]
        callback (fn [item-update]
                    (when-let [events (first
                                       (swap! market-cache-state market-cache/update-status (i/into-map item-update)))]
                      (apply f events)))]
    (new-subscription item mode fields callback)))

(defn get-item
  "Will return item used for subscription, for example MARKET:<epic>
  Is represented as String[] seq is used to get Clojure representation"
  [subscription]
  (when subscription
    (first (seq (.getItems subscription)))))

(defn get-market-data-subscription
  "Return the market item subscription for epic"
  [subscriptions epic]
  (first
   (filter #(= (i/market-item epic) (get-item %)) subscriptions)))

(defn get-account-subscription
  [subscriptions]
  (first
   (filter #(str/includes? (get-item %) "ACCOUNT:") subscriptions)))

(defn get-subscribed-epics
  "Will return seq of MARKET subscriptions "
  [subscriptions]
  (map (comp i/get-name get-item) subscriptions))
