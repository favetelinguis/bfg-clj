(ns bfg.market
(:require
  [play.strategy :as strategy]
  [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::strategies (s/coll-of ::strategy/id))