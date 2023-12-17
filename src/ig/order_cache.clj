(ns ig.order-cache
  (:require [core.events :as e]
            [ig.stream.item :as i]
            [cheshire.core :as json]
            [ig.cache :as cache]))

;; Makes sure only one order is avaliable/market

(defn- maybe-open-order
  [[_ old] change]
  (let [epic (::e/name change)
        has-order? (get old epic)]
    (if has-order?
      (cache/make old)
      (cache/make [change]
                  (assoc old epic :order-initiated)))))

(defn- remove-order
  [[_ old] change]
  (let [epic (::e/name change)
        has-order? (not (nil? (get old epic)))]
    (if-not has-order?
      (cache/make old)
      (cache/make [change]
                  (dissoc old epic)))))

(defn update-confirms
  "dealStatus ACCEPTED|REJECTED"
  [events+cache change]
  (let [[_ old] events+cache
        epic (:epic change)
        status (:dealStatus change)]
    (if (= status "REJECTED")
      (remove-order events+cache (e/exit epic))
      (if (not (nil? (get old epic)))
        (cache/make (assoc old epic :order-confirmed))
        (cache/make old)))))

(defn- update-opu
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn- update-wou
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn update-cache
  "Either I get update from stream, can be 3 different types or
   its a create order event from portfolio, only create order event should create event"
  [old event]
  (let [route (get event "ROUTE")]
    (cond
      (i/trade? route) (do
                         (when-let [data (get event "CONFIRMS")]
                           (update-confirms old (json/decode data true)))
                         (when-let [data (get event "OPU")]
                           (update-opu old (json/decode data true)))
                         (when-let [data (get event "WOU")]
                           (update-wou old (json/decode data true))))
      (= ::e/order-new
         (::e/kind event)) (maybe-open-order old event)
      (= ::e/exit
         (::e/kind event)) (remove-order old event)
      :else (println "Unsupported event order: " event))))
