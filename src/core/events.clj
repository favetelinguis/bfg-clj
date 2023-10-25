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
  [epic o h l c t]
  {::name epic
   ::open o
   ::high h
   ::low l
   ::close c
   ::time t
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
