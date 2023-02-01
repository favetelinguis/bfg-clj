(ns bfg.market.market
  (:require
    [bfg.market.signal :as signal]
    [clojure.spec.alpha :as s]))

(s/def ::id keyword?)
(defn make
  [market-id signal]
  {::id     market-id
   ::signal signal
   })

(defn update-signal
  [m bar]
  (update m ::signal signal/step-signal bar))

(defn get-signal
  [m]
  (::signal m))