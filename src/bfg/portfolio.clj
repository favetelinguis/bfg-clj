(ns bfg.portfolio
  (:require [clojure.spec.alpha :as s]
            [core.events :as event]
            [odoyle.rules :as o]))

(s/def ::update-time (s/nilable string?))
(s/def ::market-delay (s/nilable string?))
(s/def ::market-state (s/nilable #{"CLOSED" "OFFLINE" "TRADEABLE" "EDIT" "AUCTION" "AUCTION_NO_EDIT" "SUSPENDED"}))
(s/def ::bid (s/nilable string?))
(s/def ::offer (s/nilable string?))
(s/def ::account string?)
(s/def ::type #{::account-update ::trade-update})

(s/def ::trade-event (s/keys :req []))
(s/def ::account-event (s/keys :req []))

(s/def ::event (s/or ::trade-event ::account-event))

(def rules
  (o/ruleset
   {
    ::signal-update
    [:what
     [::tx-fn ::execution tx-fn]
     [::event ::signal e]
     :then
     (tx-fn {:type :signal-from-rules})]

    ::account-uppdate
    [:what
     [::tx-fn ::execution tx-fn]
     [::event ::account e]
     :then
     (tx-fn {:type :account-from-rules})]

    ::trade-update
    [:what
     [::tx-fn ::execution tx-fn]
     [::event ::trade e]
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
  "tx-fn is the function used to communicate side effect outside of session"
  [tx-fn]
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::tx-fn ::execution tx-fn)))

(defn get-all-events
  [session]
  (o/query-all session ::query-all-events))

(defn update-session
  [session {:keys [::event/kind] :as event}]
  (let [to-insert (case kind
                    ::event/signal [::event ::signal event]
                    ::event/account-update [::event ::account event]
                    ::event/trade-update [::event ::trade event]
                    [::event ::unknown event])]
    (-> session
        (o/insert to-insert)
        o/fire-rules)))
