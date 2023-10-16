(ns core.events
  (:require [clojure.spec.alpha :as s]
            [core.event.account :as account]
            [core.event.trade :as trade]
            [core.event.market :as market]))

(s/def ::kind #{::market/candle ::market/status-update ::account/update ::trade/updatel})

(defn create-event
  "TODO can I validate all created events"
  [kind base-event]
  (when (s/valid? ::kind kind)
    (merge base-event {::kind kind})))
