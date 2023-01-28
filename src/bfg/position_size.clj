(ns bfg.position-size
  (:require
    [bfg.portfolio.order :as order]
    ))

(defn constant-size-strategy
  [size]
  (fn [portfolio account market]
    (list (order/make :test-order-id :mid size :BUY 22.11))))
