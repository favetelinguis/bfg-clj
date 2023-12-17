(ns ig.order-manager.order-cache
  (:require [core.events :as e]))

(defn make [] {})

(defn has-order?
  [cache epic]
  (not (nil? (get cache epic))))

(defn add-order
  [cache {:keys [::e/name] :as event}]
  (assoc cache name event))

(defn remove-order
  [cache epic]
  (dissoc cache epic))
