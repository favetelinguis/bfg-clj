(ns ig.order-cache
  (:require [core.events :as e]
            [ig.stream.item :as i]
            [cheshire.core :as json]
            [ig.cache :as cache]))

;; Makes sure only one order is avaliable/market
;; Confirms ONLY handle rejected orders after trying to open them
;; OPU used for all other changes to orders

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
      (remove-order events+cache (e/order-create-failure epic))
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

(defn- update-account
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn- update-signal
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn update-cache
  [prev {:keys [::e/action ::e/data] :as event}]
  (case action
    "TRADE" (do
              (when-let [s (get data "CONFIRMS")]
                (update-confirms prev (json/decode s true)))
              (when-let [s (get data "OPU")]
                (update-opu prev (json/decode s true)))
              (when-let [s (get data "WOU")]
                (update-wou prev (json/decode s true))))
    "ACCOUNT" (update-account prev data)
    "SIGNAL" (update-signal prev data)
    "ORDER-CREATE-FAILURE" (remove-order prev data)
    (println "Unsupported event in order-cache: " event)))
