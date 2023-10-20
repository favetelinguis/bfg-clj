(ns core.portfolio.rules
  (:require
   [odoyle.rules :as o]
   [core.events :as e]
   [core.command :as command]))

(def rules
  (o/ruleset
   {
    ::bid-update
    [:what
     [::executors ::command c]
     [epic ::e/bid bid]
     :then
     (command/open-working-order! c bid)]

    ::ask-update
    [:what
     [::executors ::command c]
     [epic ::e/ask ask]
     :then
     (command/close-working-order! c ask)]

    ::get-bid-ask-all-markets
    [:what
     [epic ::e/bid bid]
     [epic ::e/ask ask]]
    }))

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
                    ::e/bid [epic kind event]
                    ::e/ask [epic kind event]
                    ::e/candle [epic kind event]
                    ; TODO add all event kinds
                    [::event ::unknown event])]
    (-> session
        (o/insert to-insert)
        o/fire-rules)))
