(ns bfg.portfolio
  (:import (java.time Instant)))

;; TODO only support fill-or-kill to start, how do it work with partial fills? Do each fill create its own order/position?

(defn make-order
  [id account-id market-id size direction wanted-entry-price wanted-exit-price]
  {:id           id
   :account      account-id
   :market       market-id
   :size         size
   :direction    direction
   :wanted-price wanted-entry-price
   :wanted-exit  wanted-exit-price
   })

(defn make-position
  [id entry-price entry-time]
  {:id          id
   :order       nil
   :entry-price entry-price
   :entry-time  entry-time})

(defn make-portfolio
  []
  {:orders    {}
   :positions {}
   })

(defn add-order [portfolio order]
  (update portfolio :orders assoc (:id order) order))

(defn cancel-order [portfolio order-id]
  (update portfolio :orders dissoc order-id))

(defn order->position
  "Moves order into the open position and remove it from orders"
  [portfolio order-id position]
  (when-let [order (get-in portfolio [:orders order-id])]
    (let [position-with-order (assoc position :order order)]
      (-> portfolio
          (update :orders dissoc order-id)
          (update :positions assoc (:id position-with-order) position-with-order)))))

(defn exit-position [portfolio position-id]
  (update portfolio :positions dissoc position-id))