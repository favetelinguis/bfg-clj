(ns bfg.portfolio.position
  (:require [bfg.portfolio.order :as order]
            [clojure.spec.alpha :as s]))

(s/def ::id keyword?)
(s/def ::price (s/double-in :min 0. :max 100000.0))
(s/def ::time inst?)
(s/def ::position (s/keys :req-un [::id ::time ::price (s/nilable ::order/order)]))

(defn make
  [id price time]
  {::id      id
   :price    price
   :time     time})
