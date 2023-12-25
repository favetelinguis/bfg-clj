(ns core.events
  (:require [clojure.spec.alpha :as s])
  (:import [java.time Instant]))

(s/def ::epic string?)

;; TODO define more complete specs for events

(s/def ::kind #{::trade
                ::chart
                ::market
                ::account
                ::candle
                ::mid-price
                ::balance
                ::order-new
                ::exit
                ::filled
                ::signal
                ::unsubscribed})

(defn make-event
  ([kind payload] (make-event kind payload (Instant/now)))
  ([kind payload x]
   {::kind kind
    ::ingest-time x
    ::payload payload}))

(def make-trade-event (partial make-event ::trade))
(def make-chart-event (partial make-event ::chart))
(def make-market-event (partial make-event ::market))
(def make-account-event (partial make-event ::account))

(defn create-candle-event
  [epic t h l o c]
  {::name epic
   ::time t
   ::high h
   ::low l
   ::open o
   ::close c
   ::kind ::candle})

(defn create-mid-price-event
  [epic p]
  {::name epic
   ::price p
   ::kind ::mid-price})

(defn create-balance-event
  [account-id balance]
  {::account-id account-id
   ::balance balance
   ::kind ::balance})

(s/def ::message string?)
(s/def ::type #{::parsing-error ::fatal})
(s/def ::event (s/keys :req [::message
                             ::type]))

(defn create-fatal-error
  [s]
  {::type ::fatal
   ::message s})

(defn create-new-order
  [epic direction size]
  {::name epic
   ::size size
   ::direction direction
   ::kind ::order-new})

(defn exit
  [epic]
  {::name epic
   ::kind ::exit})

(defn position-new
  [epic direction size]
  {::name epic
   ::size size
   ::direction direction
   ::kind ::position-new})

(defn unsubscribe-new
  [epic]
  {::name epic
   ::kind ::unsubscribed})

(defn signal-new
  [epic direction]
  {::name epic
   ::direction direction
   ::kind ::signal})

(s/def ::action #{; Events
                  ::order-updated
                  ; Commands
                  ::create-order
                  ; OLD
                  ::trade
                  ::chart
                  ::market
                  ::account
                  ::candle
                  ::mid-price
                  ::balance
                  ::order-new
                  ::exit
                  ::filled
                  ::signal
                  ::unsubscribed})

(defn make-event
  [action data]
  {::id (random-uuid)
   ::action action
   ::data data
   ::timestamp (Instant/now)})

(defn make-command
  [action data parent-id]
  {::id (random-uuid)
   ::parent-id parent-id
   ::action action
   ::data data
   ::timestamp (Instant/now)})
