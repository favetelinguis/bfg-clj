(ns app.strategy-store
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [core.signal :as signal]
            [ig.stream.connection :as stream]
            [ig.stream.subscription :as subscription]
            [ig.stream.item :as i]))

(defrecord StrategyStore [state application stream]
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

(defn add
  "sig is impl protocol Signal
  markets is seq of epic names for wich to subscribe signal to"
  [store sig markets]
  (let [{:keys [connection]} (:stream store)
        {:keys [make-strategy send-to-app!!]} (:application store)
        {:keys [state]} store
        candle-sub (fn [m] (subscription/new-candle-subscription (i/chart-candle-1min-item m)
                                                                 send-to-app!!))
        market-sub (fn [m] (subscription/new-market-subscription m))
        subscribed-markets (->> (keys @state)
                                (map #(second (clojure.string/split % #"_")))
                                (into #{}))
        setup-strategy! (fn [market]
                          (let [c (make-strategy signal/on-update sig market)]
                            (swap! state assoc (str (signal/get-name sig) "_" market) c)))]
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
