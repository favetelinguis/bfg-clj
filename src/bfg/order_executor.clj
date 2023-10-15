(ns bfg.order-executor)

(defprotocol OrderExecutor
  "A set of functions for creating orders"
  (open-working-order! [order] "Used to open new working order")
  (close-working-order! [order] "Used to close a working order"))

(deftype DummyOrderExecutor []
    OrderExecutor
  (open-working-order! [order]
    (println "Open new working order"))

  (close-working-order! [order]
    (println "Close working order")))
