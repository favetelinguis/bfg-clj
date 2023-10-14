(ns app.market-generator
  (:require
   [bfg.error :as error]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]))

(defn start-market-generator
  [signal-generator state]
  (println "Starting MarketGenerator")
  (let [in-channel (a/chan)
        tx (get-in signal-generator [:rx])]
    (a/thread
      (try
        (loop []
          (when-let [event (a/<!! in-channel)]
            (println "In MarketGenerator: " event signal-generator)
            (swap! state conj event)
            (a/>!! tx {:type :market}) ; should import event and validate from singnal
            (recur)))
        (catch Throwable e (a/>!! tx (error/create-fatal-error (ex-message e)))))
      (println "Shutting down MarketGenerator"))
    in-channel))

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
