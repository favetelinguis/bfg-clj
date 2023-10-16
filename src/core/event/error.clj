(ns core.event.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::type #{::parsing-error ::fatal})
(s/def ::event (s/keys :req [::message
                             ::type]))

(defn create-fatal-error
  [message]
  {::type ::fatal
   ::message message})
