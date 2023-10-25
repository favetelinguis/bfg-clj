(ns core.portfolio.rules
  (:require
   [odoyle.rules :as o]
   [core.events :as e]
   [core.command :as command]))

(def rules
  (o/ruleset
   {::mid-price-update
    [:what
     [::executors ::command c]
     [epic ::e/mid-price price]
     :then
     (command/open-working-order! c price)]

    ::candle-update
    [:what
     [::executors ::command c]
     [epic ::e/candle candle]
     :then
     (command/close-working-order! c candle)]

    ::balance-update
    [:what
     [::executors ::command c]
     [epic ::e/balance balance]
     :then
     (command/close-working-order! c balance)]

    ::get-test
    [:what
     [epic ::e/mid-price price]]}))

(defn create-session
  "command-executor is an impl of protocol CommandExecutor"
  [command-executor]
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::executors ::command command-executor)))

(defn get-bid-ask-all-markets
  [session]
  (o/query-all session ::get-bid-ask-all-markets))

(defn update-session
  [session {:keys [::e/kind ::e/epic ::e/account] :as event}]
  (let [to-insert (case kind
                    ::e/mid-price [epic kind event]
                    ::e/candle [epic kind event]
                    ::e/balance [account kind event]
                    ; TODO add all event kinds
                    [::event ::unknown event])]
    (-> session
        (o/insert to-insert)
        o/fire-rules)))
