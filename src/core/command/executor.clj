(ns core.command.executor
  (:require [clojure.core.async :as a]
            [core.event.error :as error]))

(defn start
  [command-executor-impl]
  (println "Starting CommandExecutor")
  (let [rx (a/chan)]
    (a/thread
      (try
        (loop []
          (when-let [command (a/<!! rx)]
            (println "In CommandExecutor: " command)
            ; TODO use protocol and command type decide action
            (recur)))
        (catch Throwable e (println (error/create-fatal-error (ex-message e)))))
      (println "Shutting down CommandExecutor"))
    rx))


(defprotocol CommandExecutor
  "A set of functions for creating orders
  Each function will execute in the portfolio, it is very important not to block since that will
  prevent portfolio from processing data. Each function in CommandExecutor should execute
  in an async way to not block portfolio."
  (open-working-order! [this order] "Used to open new working order")
  (close-working-order! [this order] "Used to close a working order"))

(deftype DummyCommandExecutor []
    CommandExecutor
  (open-working-order! [this order]
    (println "Open new working order"))

  (close-working-order! [this order]
    (println "Close working order")))
