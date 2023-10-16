(ns core.market.executor
  (:require [clojure.core.async :as a]
            [core.event.error :as error]))

(defn start-market-generator
  [out-component state]
  (println "Starting MarketGenerator")
  (let [in-channel (a/chan)
        tx (get-in out-component [:rx])]
    (a/thread
      (try
        (loop []
          (when-let [event (a/<!! in-channel)]
            (println "In MarketGenerator: " event)
            (swap! state conj event)
            (a/>!! tx {:type :market}) ; should import event and validate from singnal
            (recur)))
        (catch Throwable e (a/>!! tx (error/create-fatal-error (ex-message e)))))
      (println "Shutting down MarketGenerator"))
    in-channel))
