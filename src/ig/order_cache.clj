(ns ig.order-cache
  (:require [core.events :as e]
            [ig.stream.item :as i]
            [cheshire.core :as json]))

;; This cache is part of the stateful transducers it must have the function signature
;; (fn [old-cache event] [[::e/events to propagate] updated-cache])

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn update-confirms
  "Signal when order is acc"
  [old change]
  (let [account (i/get-name (get change "ROUTE"))]
    (make
     (update old account merge change))))

(defn update-opu
  "Signal when order goes to position"
  [old change]
  (let [account (i/get-name (get change "ROUTE"))]
    (make
     (update old account merge change))))

(defn update-wou
  [old change]
  (let [account (i/get-name (get change "ROUTE"))]
    (make
     (update old account merge change))))

(defn maybe-open-order
  [old change]
  (let [account (i/get-name (get change "ROUTE"))]
    (make
     [] ; TODO logic to maybe open order
     (update old account merge change))))

(defn update-cache [old event]
  "Either I get update from stream, can be 3 different types or
   its a create order event from portfolio, only create order event should create event"
  (let [route (get event "ROUTE")]
    (cond
      (i/trade? route) (let []
                         (when-let [data (get event "CONFIRMS")]
                           (update-confirms old (json/decode data true)))
                         (when-let [data (get event "OPU")]
                           (update-opu old (json/decode data true)))
                         (when-let [data (get event "WOU")]
                           (update-wou old (json/decode data true))))
      (= ::e/order-new
         (::e/kind event)) (maybe-open-order old event)
      :else (println "Unsupported event: " event))))
