(ns ig.order-manager
  (:require [core.events :as e]
            [ig.order-manager.order-cache :as cache]
            [clojure.core.async :as a]))

(defn start
  [rx portfolio-fn order-state command-executor]
  (println "Starting OrderManager")
  (a/thread
    (try
      (loop []
        (when-let [event (a/<!! rx)]
                                        ; TODO
                                        ; Input from portfolio AND stream
                                        ; If from Portfolio its a command and command-executor should be called but first check cache if ok and update
                                        ; If from stream its an event and we should update cachec and create event to Portfolio
          #_(swap! order-state + event)
          (println event)
          (recur)))
      (println "Shutting down OrderManager")
      (catch Throwable e (println "Error in OrderManager: " (.getMessage e))))))
