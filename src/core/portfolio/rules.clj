(ns core.portfolio.rules
  (:require [clojure.spec.alpha :as s]
            [odoyle.rules :as o]
            [core.events :as event]
            [core.event.market :as market]
            [core.event.trade :as trade]
            [core.event.account :as account]))

(def rules
  (o/ruleset
   {
    ::signal-update
    [:what
     [::tx-fn ::execution tx-fn]
     [epic ::signal e]
     :then
     (tx-fn {:type :signal-from-rules})]

    ::account-uppdate
    [:what
     [::tx-fn ::execution tx-fn]
     [account ::account e]
     :then
     (tx-fn {:type :account-from-rules})]

    ::trade-update
    [:what
     [::tx-fn ::execution tx-fn]
     [epic ::trade e]
     :then
     (tx-fn {:type :trade-from-rules})]

    ::unknown-update
    [:what
     [::tx-fn ::execution tx-fn]
     [::event ::unknown e]
     :then
     (tx-fn {:type :unknown-from-rules})]

    ::query-all-events
    [:what
     [::event ::trade-event te]
     [::event ::account-event ae]]
    }))

(defn create-session
  "tx-fn is the function used to communicate side effect outside of session
  it will send out the following events ::order/open..."
  [tx-fn]
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::tx-fn ::execution tx-fn)))

(defn get-all-events
  [session]
  (o/query-all session ::query-all-events))

(defn update-session
  [session {:keys [::event/kind] :as event}]
  (let [to-insert (case kind
                    ::market/status-update [(::market/epic event) event]
                    ::market/candle [(::market/epic event) event]
                    ::account/update [(::account/name event) event]
                    ::trade/update [(::trade/epic event) event]
                    [::event ::unknown event])]
    (-> session
        (o/insert to-insert)
        o/fire-rules)))
