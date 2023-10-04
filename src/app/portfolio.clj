(ns app.portfolio
  (:require
   [bfg.error :as error]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [bfg.portfolio :as portfolio]))

(defn start-portfolio
  [*session]
  (println "Starting Portfolio")
  (let [rx (a/chan)]
    (a/thread
      (try
        (loop []
          (when-let [event (a/<!! rx)]
            (println "In Portfolio: " event)
            (swap!  *session portfolio/update-session event)
            (recur)))
        (catch Throwable e (swap! *session
                                  portfolio/update-session
                                  (error/create-fatal-error (ex-message e)))))
      (println "Shutting down Portfolio"))
    rx))

(defrecord Portfolio [rx session]
  component/Lifecycle
  (start [this]
    (if rx
      this
      (let [in-channel (start-portfolio session)]
        (assoc this :rx in-channel))))
  (stop [this]
    (if rx
      (a/close! rx) ; closing the in-channel will cause the thread to exit
      (assoc this :rx nil))))

(defn new-portfolio []
  (map->Portfolio {:session (atom
                             (portfolio/create-session
                              (fn [event] (println "Im the tx-fn will be a channel"))))}))
