(ns core.events
  (:require [clojure.spec.alpha :as s]))

(s/def ::epic string?)

;; TODO define more complete specs for events

(s/def ::kind #{::candle
                ::mid-price
                ::balance
                ::order-new
                ::order-update
                ::fill
                ::position-new
                ::position-update
                ::position-exit})

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
  [epic direction]
  {::name epic
   ::uuid (str (java.util.UUID/randomUUID))
   ::direction direction
   ::kind ::order-new})
