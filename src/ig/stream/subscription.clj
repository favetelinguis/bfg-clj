(ns ig.stream.subscription
  (:require
   [ig.stream.item :as i]
   [ig.order-manager :as order-manager]
   [ig.market-cache :as market-cache]
   [clojure.string :as str]
   [core.events :as e]
   [cheshire.core :as json])
  (:import (com.lightstreamer.client Subscription SubscriptionListener)))

(defn- new-subscription-listener [callback]
  (reify
    SubscriptionListener
    (onSubscription [this] (println "onSubscription"))
    ;; (onListenStart [this subscription] (println subscription))
    ;; (onListenEnd [this subscription] (println subscription))
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
        fields ["MARKET_DELAY" "MARKET_STATE"]
        callback (fn [item-update]
                   (when-let [events (first
                                      (swap! market-cache-state market-cache/update-status (i/into-map item-update)))]
                     (apply f events)))]
    (new-subscription item mode fields callback)))

(defn new-candle-subscription
  [item f market-cache-state]
  (let [mode "MERGE"
        fields ["OFR_OPEN", "OFR_HIGH", "OFR_LOW", "OFR_CLOSE", "BID_OPEN", "BID_HIGH", "BID_LOW", "BID_CLOSE", "CONS_END", "UTM"]
        callback (fn [item-update]
                   (when-let [events (first
                                      (swap! market-cache-state market-cache/update-candle (i/into-map item-update)))]
                     (apply f events)))]
    (new-subscription item mode fields callback)))

(defn new-account-subscription
  [account-id f]
  (let [item (i/account-item account-id)
        mode "MERGE"
        fields ["AVAILABLE_CASH"]
        callback (fn [item-update] (let [m (i/into-map item-update)
                                         balance (get m "AVAILABLE_CASH")
                                         event (e/create-balance-event account-id (Double/parseDouble balance))]
                                     (f event)))]
    (new-subscription item mode fields callback)))

(defn new-trade-subscription
  [account-id f]
  (let [item (i/trade-item account-id)
        mode "DISTINCT"
        fields ["CONFIRMS" "OPU" "WOU"]
        callback (fn [item-update] (let [m (i/into-map item-update)]
                                     (when-let [data (get m "CONFIRMS")]
                                       (f (-> (json/decode data true)
                                              (assoc ::order-manager/kind :confirms))))
                                     (when-let [data (get m "OPU")]
                                       (f (-> (json/decode data true)
                                              (assoc ::order-manager/kind :opu))))
                                     (when-let [data (get m "WOU")]
                                       (f (-> (json/decode data true)
                                              (assoc ::order-manager/kind :wou))))))]
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
