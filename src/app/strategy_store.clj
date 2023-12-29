(ns app.strategy-store
  (:require [com.stuartsierra.component :as component]
            [ig.application :refer [send-to-app!! make-strategy]]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [core.signal :as signal]
            [ig.stream.connection :as stream]
            [ig.stream.subscription :as subscription]
            [ig.stream.item :as i]
            [ig.cache :as cache]))

(def strategies
  {"DAXKiller" [signal/dax-killer {:bars 0}]
   "DAXKiller2" [signal/make-dax-killer-signal {}]
   "DAXKiller3" [signal/make-dax-killer-signal {}]})

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
  "sig is string pointing to what strategy to use
  markets is seq of epic names for wich to subscribe signal to"
  [store sig markets]
  (let [{:keys [connection]} (:stream store)
        [strategy-fn initial-state] (get strategies sig)
        {:keys [state]} store
        candle-sub (fn [m] (subscription/new-candle-subscription (i/chart-candle-1min-item m)
                                                                 send-to-app!!))
        market-sub (fn [m] (subscription/new-market-subscription m send-to-app!!))
        subscribed-markets (->> (keys @state)
                                (map #(second (clojure.string/split % #"_")))
                                (into #{}))
        setup-strategy! (fn [market]
                          (let [key (str sig "_" market)
                                kill-fn (make-strategy strategy-fn (cache/make initial-state) market)]
                            (swap! state assoc key kill-fn)))]
    (doseq [market markets]
      (setup-strategy! market)
      (when-not (contains? subscribed-markets market)
        (stream/subscribe! connection
                           (market-sub market)
                           (candle-sub market))))))

(defn delete
  "strategy name string and market name string
  delete strategy from state and unsibscribe if it the only strategy for epic"
  [store s]
  (let [{:keys [state]} store
        {:keys [connection]} (:stream store)]
    (when-let [kill-fn (get @state s)]
      (kill-fn)
      (swap! state dissoc s)
      (let [[_ epic] (clojure.string/split s #"_")]
        (when (empty? (filter #(clojure.string/ends-with? % epic) (keys @state)))
          (let [subs (stream/get-subscriptions connection)
                market-sub (first (subscription/get-subscriptions-matching subs (i/market-item epic)))
                candle-sub (first (subscription/get-subscriptions-matching subs (i/chart-candle-1min-item epic)))]
            (stream/unsubscribe! connection market-sub candle-sub)))))))
