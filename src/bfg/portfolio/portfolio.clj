(ns bfg.portfolio.portfolio
  (:require [bfg.portfolio.order :as order]
            [bfg.portfolio.position :as position])
  )

(defn make
  []
  {::orders    {}
   ::positions {}
   })

(defn add-order
  [p order]
  (update p ::orders assoc (::order/id order) order))

(defn cancel-order [p {:keys [order-id]}]
  (update p ::orders dissoc order-id))

(defn order->position
  "Moves order into the open position and remove it from orders"
  [p {:keys [order-id position]}]
  (when-let [order (get-in p [::orders order-id])]
    (let [position-with-order (assoc position ::position/order order)]
      (-> p
          (update ::orders dissoc order-id)
          (update ::positions assoc (:position/id position-with-order) position-with-order)))))

(defn exit-position [p {:keys [position-id]}]
  (update p ::positions dissoc position-id))

(defn get-order [p order-id]
  (get-in p [::orders order-id]))

(defn get-position [p position-id]
  (get-in p [::positions position-id]))

(defn get-positions [p]
  (::positions p))

(defn get-orders [p]
  (::orders p))
