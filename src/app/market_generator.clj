(ns app.market-generator
  (:require
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]))

(defn start-market-generator
  [{:keys [rx :as tx]} state]
  (println "Starting MarketGenerator")
  (let [rx (a/chan)]
    (a/thread
      (loop []
        (when-let [event (a/<!! rx)]
          (println "In MarketGenerator: " event)
          (swap! state conj event)
          (a/>!! tx {:type :market}) ; should import event and validate from singnal
          (recur)))
      (println "Shutting down MarketGenerator"))
    rx))

(defrecord MarketGenerator [rx signal-generator state]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (start-market-generator signal-generator state)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the market thread to exit
      (assoc this :rx nil))))

(defn new-market-generator []
  (map->MarketGenerator {:state (atom [])}))
