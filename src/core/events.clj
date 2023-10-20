(ns core.events
  (:require [clojure.spec.alpha :as s]))

(s/def ::epic string?)

(s/def ::bid double?)
(s/def ::offer double?)

(s/def ::kind #{::candle
                ::bid ; remove
                ::ask ; remove
                ::mid-price
                ::balance
                ::order-new
                ::order-update
                ::fill
                ::position-new
                ::position-update
                ::position-exit})

;; TODO define more complete specs for events

(defn create-bid-event
  [epic bid]
  {::epic epic
   ::bid bid
   ::kind ::bid})

(defn create-ask-event
  [epic ask]
  {::epic epic
   ::ask ask
   ::kind ::ask})

(s/def ::message string?)
(s/def ::type #{::parsing-error ::fatal})
(s/def ::event (s/keys :req [::message
                             ::type]))

(defn create-fatal-error
  [s]
  {::type ::fatal
   ::message s})
