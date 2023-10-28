(ns core.portfolio.rules
  (:require
   [core.signal :as signal]
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

    ;; ::run-signal-on-midprice TODO
    ::run-signal-on-candle
    [:what
     [epic ::e/candle candle]
     [epic ::signal-activated id]
     [id ::signal/runner runner]
     :then
     (when-let [update (signal/on-candle runner candle)]
       (o/insert! update))]

    ::active-signal
    [:what
     [epic ::signal-activated id]
     [id ::signal/state ::signal/inactive]
     :then
     (println "Active signal")
     (o/insert! id ::signal/state ::signal/active)]

    ::get-all-signals
    [:what
     [id ::signal/state state]
     [id ::signal/name name]]}))

(defn create-session
  "command-executor is an impl of protocol CommandExecutor"
  [command-executor signals]
  (-> (reduce o/add-rule (o/->session) rules)
      (#(reduce (fn [session {:keys [::signal/id] :as vals}] (o/insert session id vals)) % signals)) ; Add in all strategies
      (o/insert ::executors ::command command-executor)
      o/fire-rules))

(defn get-signals
  "[{:id #uuid \"478aadec-10bd-42f5-9494-2ea17ca84dc0\", :s :core.strategy/inactive, :n DAX Killer}]"
  [session]
  (o/query-all session ::get-all-signals))

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

(defn activate-signal-for-market
  "Insert signal for epic will not fire rules since signal only fires on-candle and on-midprice"
  [session id epic]
  (-> session
      (o/insert epic ::signal-activated id)
      (o/fire-rules)))
