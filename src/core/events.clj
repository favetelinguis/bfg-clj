(ns core.events
  (:require [clojure.spec.alpha :as s])
  (:import [java.time Instant]))

(s/def ::action #{"PANIC"
                  "UNSUBSCRIBE"
                  "SUBSCRIBE"
                  "MARKET"
                  "TRADE"
                  "ACCOUNT"
                  "CHART"
                  "BALANCE"
                  "SIGNAL"
                  "ORDER-FAILURE"
                  "UPDATE-ORDER"})

(defn make-event
  [action data]
  {::id (random-uuid)
   ::action action
   ::data data
   ::timestamp (Instant/now)})

(def panic (partial make-event "PANIC"))
(s/fdef panic
  :args (s/cat :data (s/keys :req [::reason])))

(def unsubscribe (partial make-event "UNSUBSCRIBE"))
(s/fdef unsubscribe
  :args (s/cat :data (s/keys :req [::name ::route])))

(def subscribe (partial make-event "SUBSCRIBE"))
(s/fdef subscribe
  :args (s/cat :data (s/keys :req [::name ::route])))

(def stream-update make-event)
(s/fdef stream-update
  :args (s/cat :route string? :data (s/keys :req [::name])))

(def balance (partial make-event "BALANCE"))
(s/fdef balance
  :args (s/cat :data (s/keys :req [::name ::balance])))

(def signal-update make-event)
(s/def ::direction #{"BUY" "SELL"})
(s/fdef signal-update
  :args (s/cat :route string? :data (s/keys :req [::name]
                                            :opt [::time ::high ::low ::open ::close ::mid-price])))

(def signal (partial make-event "SIGNAL"))
(s/fdef signal
  :args (s/cat :data (s/keys :req [::name ::direction])))

(def update-order (partial make-event "UPDATE-ORDER"))
(s/fdef update-order
  :args (s/cat :data (s/keys :req [::name ::size ::direction])))

(def order-failure (partial make-event "ORDER-FAILURE"))
(s/fdef order-failure
  :args (s/cat :data (s/keys :req [::name ::status-code ::reason ::request])))
