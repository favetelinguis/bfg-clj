(ns ig.stream.subscription
  (:require
   [ig.stream.item :as i]
   [ig.market-cache :as market-cache]
   [clojure.string :as str]
   [core.events :as e]
   [cheshire.core :as json])
  (:import (com.lightstreamer.client Subscription SubscriptionListener)))

(defn- new-subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (println "onSubscription"))
    (onListenStart [this subscription] (println "onListenStart"))
    (onListenEnd [this subscription] (println "onListenEnd")) ;; can i use to delete in cache when market is shut down
    (onItemUpdate [this item-update] (callback item-update))
    (onSubscriptionError [this code message] (println (str code ": " message)))))

(defn- new-subscription
  [item mode fields callback]
  (let [subscription (Subscription.  mode (into-array String [item]) (into-array String fields))]
    (doto subscription
      (.addListener (new-subscription-listener callback)))))

(defn new-market-subscription
  "f is the callback that takes item-update"
  [epic f]
  (let [item (i/market-item epic)
        mode "MERGE"
        callback (fn [item-update] (f (i/into-map item-update)))
        fields ["MARKET_DELAY" "MARKET_STATE"]]
    (new-subscription item mode fields callback)))

(defn new-candle-subscription
  [item f]
  (let [mode "MERGE"
        callback (fn [item-update] (f (i/into-map item-update)))
        fields ["OFR_OPEN", "OFR_HIGH", "OFR_LOW", "OFR_CLOSE", "BID_OPEN", "BID_HIGH", "BID_LOW", "BID_CLOSE", "CONS_END", "UTM"]]
    (new-subscription item mode fields callback)))

(defn new-account-subscription
  [account-id f]
  (let [item (i/account-item account-id)
        mode "MERGE"
        fields ["AVAILABLE_CASH"]
        callback (fn [item-update] (f (i/into-map item-update)))]
    (new-subscription item mode fields callback)))

(defn new-trade-subscription
  [account-id f]
  (let [item (i/trade-item account-id)
        mode "DISTINCT"
        fields ["CONFIRMS" "OPU" "WOU"]
        callback (fn [item-update] (let [m (i/into-map item-update)]
                                     (when-let [data (get m "CONFIRMS")]
                                       (f (-> (json/decode data true)
                                              (assoc ::e/kind :confirm))))
                                     (when-let [data (get m "OPU")]
                                       (f (-> (json/decode data true)
                                              (assoc ::e/kind :opu))))
                                     (when-let [data (get m "WOU")]
                                       (f (-> (json/decode data true)
                                              (assoc ::e/kind :wou))))))]
    (new-subscription item mode fields callback)))

(defn get-item
  "Will return item used for subscription, for example MARKET:<epic>
  Is represented as String[] seq is used to get Clojure representation"
  [subscription]
  (when subscription
    (first (seq (.getItems subscription)))))

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
