(ns bfg.portfolio
  (:require [clojure.spec.alpha :as s]
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
   {::update-account
    [:what
     [::tx-fn ::execution tx-fn]
     [::event ::signal e]
     :then
     (tx-fn {:type :output-from-rules})]

    ::query-all-events
    [:what
     [::event ::trade-event te]
     [::event ::account-event ae]]
    }))

(defn create-session
  [tx-fn]
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::tx-fn ::execution tx-fn)))

(defn update-session
  [session {:keys [type] :as event}]
  (-> session
      (o/insert ::event ::signal event) ; TODO will have to figure out a good way to insert and detect different events
      o/fire-rules))

(defn get-all-events
  [session]
  (o/query-all session ::query-all-events))
