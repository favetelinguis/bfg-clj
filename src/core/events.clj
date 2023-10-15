(ns core.events
  (:require [clojure.spec.alpha :as s]))


(s/def ::kind #{::market-update ::candle-update ::account-update ::trade-update ::signal})

(defn create-event
  "TODO can I validate all created events"
  [kind base-event]
  (when (s/valid? ::kind kind)
    (merge base-event {::kind kind})))
