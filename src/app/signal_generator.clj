(ns app.signal-generator
  (:require
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [bfg.error :as error]))

(defn start-signal-generator
  [{:keys [rx :as tx]} state]
  (println "Starting SignalGenerator")
  (let [rx (a/chan)]
    (a/thread
      (try
        (loop []
          (when-let [event (a/<!! rx)]
            (println "In SignalGenerator: " event)
            (swap! state conj event)
            (a/>!! tx {:type :signal}) ; should import event and validate from singnal
            (recur)))
        (catch Throwable e (a/>!! tx (error/create-fatal-error (ex-message e)))))
      (println "Shutting down SignalGenerator"))
    rx))

(defrecord SignalGenerator [rx portfolio state]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (start-signal-generator portfolio state)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new-signal-generator []
  (map->SignalGenerator {:state (atom [])}))
