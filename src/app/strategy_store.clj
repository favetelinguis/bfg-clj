(ns app.strategy-store
  (:require [com.stuartsierra.component :as component]
            [app.transducer-utils :as utils]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [ig.stream.connection :as stream]
            [ig.stream.subscription :as subscription]
            [ig.stream.item :as i]))

(defrecord StrategyStore [state instrument-store port stream]
  component/Lifecycle
  (start [this]
    (println "Starting StrategyStore")
    (if state
      this
      (let [state (atom {})]
        (-> this
            (assoc :state state)))))
  (stop [this]
    (println "Stoping StrategyStore")
    (when state
      (assoc this :state nil))))

(defn make []
  (map->StrategyStore {}))

(defn make-strategy
  "f should be a function of form (fn [prev-state event] [[<sig events>] new-state])"
  [f start-state]
  (utils/make-state-transducer f start-state))

(defn add
  "s is the string name of the strategy
  markets are epic names as strings"
  [store s strategy markets]
  (let [{:keys [market-topic]} (:instrument-store store)
        {:keys [mix]} (:portfolio store)
        {:keys [connection channel]} (:stream store)
        {:keys [state]} store
        candle-sub (fn [m] (subscription/new-candle-subscription (i/chart-candle-1min-item m)
                                                                 (fn [event]
                                                                   (a/>!! channel event))))
        market-sub (fn [m] (subscription/new-market-subscription m (fn [event]
                                                                     (a/>!! channel event))))
        subscribed-markets (->> (keys @state)
                                (map #(second (clojure.string/split % #"_")))
                                (into #{}))
        setup-strategy! (fn [m]
                          (let [c (a/chan 1 strategy utils/ex-fn)]
                            (a/sub market-topic m c)
                            (a/admix mix c)
                            (swap! state assoc (str s "_" m) c)))]
    (doseq [market markets]
      (setup-strategy! market)
      (when-not (contains? subscribed-markets market)
        (stream/subscribe! connection
                           (market-sub market)
                           (candle-sub market))))))

(defn delete
  "strategy name string and market name string
  delete strategy from state and unsibscribe if it the only strategy for epic"
  [store strategy epic]
  (let [{:keys [state]} store
        {:keys [mix]} (:port store)
        {:keys [connection]} (:stream store)]
    (when-let [c (get-in @state [(str strategy "_" epic) :channel])]
      (a/unmix mix c) ; TODO dont think i need to unmix a closed channel
      (a/close! c)
      (swap! state dissoc (str strategy "_" epic))
      (when (empty? (filter #(clojure.string/ends-with? % epic) @state))
        (let [subs (stream/get-subscriptions connection)
              market-sub (first (subscription/get-subscriptions-matching subs (i/market-item epic)))
              candle-sub (first (subscription/get-subscriptions-matching subs (i/chart-candle-1min-item epic)))]
          (stream/unsubscribe! connection market-sub candle-sub))))))
