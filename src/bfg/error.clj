(ns bfg.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::type #{::parsing-error})
(s/def ::event (s/keys :req [::message
                             ::type]))
