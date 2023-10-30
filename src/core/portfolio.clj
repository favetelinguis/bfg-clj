(ns core.portfolio
  (:require [core.portfolio.rules :as rules]
            [core.events :as e]
            [clojure.core.async :as a]))

(defn start
  [rx rules-state]
  (println "Starting Portfolio")
  (a/thread
    (try
      (loop []
        (when-let [event (a/<!! rx)]
          (swap! rules-state rules/update-session event)
          (recur)))
      (println "Shutting down Portfolio")
      (catch Throwable e (swap! rules-state rules/update-session
                                (e/create-fatal-error (ex-message e)))))))
