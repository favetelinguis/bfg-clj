(ns core.command)

(defprotocol CommandExecutor
  "A set of functions for creating orders
  Each function will execute in the portfolio, it is very important not to block since that will
  prevent portfolio from processing data. Each function in CommandExecutor should execute
  in an async way to not block portfolio."
  (open-order! [this order] "Used to open new working order")
  (close-order! [this order] "Used to close a working order"))

(deftype DummyCommandExecutor []
  CommandExecutor
  (open-order! [this order]
    (println "Open new working order"))

  (close-order! [this order]
    (println "Close working order")))
