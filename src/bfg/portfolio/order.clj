(ns bfg.portfolio.order
  (:require [clojure.spec.alpha :as s]))

(s/def ::id keyword?)
(s/def ::size (s/double-in :min 0. :max 100.0))
(s/def ::price (s/double-in :min 0. :max 100000.0))
(s/def ::type #{:working-order})
(s/def ::direction #{:buy :sell})
(s/def ::order (s/keys :req-un [::id ::size ::direction ::price ::type]))

(defn make
  [id market-id size direction price]
  {::id        id
   ::size      size
   ::direction direction
   ::price     price
   ::type :working-order
   })