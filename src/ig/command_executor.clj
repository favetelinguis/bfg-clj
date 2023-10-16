(ns ig.command-executor
  (:require [core.command.executor :refer [CommandExecutor]]))

(deftype IgCommandExecutor [client]
    CommandExecutor
  (open-working-order! [this order]
    (println "Open new working order"))

  (close-working-order! [this order]
    (println "Close working order")))
