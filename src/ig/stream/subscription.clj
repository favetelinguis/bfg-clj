(ns ig.stream.subscription
  (:require
   [ig.stream.item :as i]
   [clojure.string :as str]
   [core.events :as e])
  (:import (com.lightstreamer.client Subscription SubscriptionListener)))

(defn get-item
  "Will return item used for subscription, for example MARKET:<epic>
  Is represented as String[] seq is used to get Clojure representation"
  [subscription]
  (when subscription
    (first (seq (.getItems subscription)))))

(defn- new-subscription-listener
  [item callback]
  (reify
    SubscriptionListener
    (onUnsubscription [this] (callback {"ROUTE" (str "UNSUBSCRIBE:" item)}))
    (onItemUpdate [this item-update] (callback (i/into-map item-update)))
    (onSubscriptionError [this code message] (println (str code ": " message " for " item)))))

(defn- new-subscription
  [item mode fields callback]
  (let [subscription (Subscription.  mode (into-array String [item]) (into-array String fields))]
    (doto subscription
      (.addListener (new-subscription-listener item callback)))))

(defn new-market-subscription
  "f is the callback that takes item-update"
  [epic f]
  (let [item (i/market-item epic)
        mode "MERGE"
        fields ["MARKET_DELAY" "MARKET_STATE"]]
    (new-subscription item mode fields f)))

(defn new-candle-subscription
  [item f]
  (let [mode "MERGE"
        fields ["OFR_OPEN", "OFR_HIGH", "OFR_LOW", "OFR_CLOSE", "BID_OPEN", "BID_HIGH", "BID_LOW", "BID_CLOSE", "CONS_END", "UTM"]]
    (new-subscription item mode fields f)))

(defn new-account-subscription
  [account-id f]
  (let [item (i/account-item account-id)
        mode "MERGE"
        fields ["AVAILABLE_CASH"]]
    (new-subscription item mode fields f)))

(defn new-trade-subscription
  [account-id f]
  (let [item (i/trade-item account-id)
        mode "DISTINCT"
        fields ["CONFIRMS" "OPU" "WOU"]]
    (new-subscription item mode fields f)))

(defn get-subscribed-epics
  "Will return seq of MARKET subscriptions "
  [subscriptions]
  (->> subscriptions
       (map get-item)
       (filter (fn [item] (str/includes? item "MARKET:")))
       (map i/get-name)))

(defn get-subscriptions-matching
  [subscriptions item]
  (filter (fn [subscription] (= (get-item subscription) item)) subscriptions))
