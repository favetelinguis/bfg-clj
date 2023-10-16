(ns core.portfolio.executor
  (:require [core.portfolio.rules :as rules]
            [clojure.core.async :as a]))

(defn start
  [*session]
  (println "Starting Portfolio")
  (let [rx (a/chan)]
    (a/thread
      (try
        (loop []
          (when-let [event (a/<!! rx)]
            (println "In Portfolio: " event)
            (swap!  *session rules/update-session event)
            (recur)))
        (catch Throwable e (swap! *session
                                  rules/update-session
                                  {:exception (ex-message e)})))
      (println "Shutting down Portfolio"))
    rx))
