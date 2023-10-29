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
     [::executors ::command c {:then false}]
     [epic ::e/balance balance]
     :then
     (command/close-working-order! c balance)]

    ::run-signal-on-midprice
    [:what
     [epic ::e/mid-price price]
     [epic ::signal-activated id {:then false}]
     [id ::signal data {:then false}]
     :then
     (o/insert! id ::signal (signal/on-midprice data price))]

    ::run-signal-on-candle
    [:what
     [epic ::e/candle candle]
     [epic ::signal-activated id {:then false}]
     [id ::signal data {:then false}] ; to avoid infinite loops
     :then
     (o/insert! id ::signal (signal/on-candle data candle))]

    ::check-commands-to-execute
    [:what
     [::executors ::command c {:then false}]
     [id ::signal data]
     :then
     (when-let [command (signal/get-commands data)]
       (command/open-working-order! c command))]

    ::get-active-signals
    [:what
     [epic ::signal-activated signal-id]
     [signal-id ::signal data]]

    ::get-all-signals
    [:what
     [signal-id ::signal data]]}))

(defn create-session
  "command-executor is an impl of protocol CommandExecutor
  signals is a list of impl of core.signal/Signal which we add an id to in the method"
  [command-executor signals & {:keys [id-fn] :or {id-fn #(str (java.util.UUID/randomUUID))}}]
  (-> (reduce o/add-rule (o/->session) rules)
      (#(reduce
         (fn [session signal] (o/insert session (id-fn) ::signal signal))
         %
         signals)) ; Add in all strategies
      (o/insert ::executors ::command command-executor)
      o/fire-rules))

(defn get-all-signals
  [session]
  (let [active-signal-ids (->> (o/query-all session ::get-active-signals)
                               (map :signal-id)
                               (into #{}))]
    (map (fn [m] (-> (if (active-signal-ids (:signal-id m))
                       (assoc m ::signal/active? true)
                       (assoc m ::signal/active? false))
                     (assoc ::signal/id (:signal-id m))
                     (assoc ::signal/name (signal/get-name (:data m)))))
         (o/query-all session ::get-all-signals))))

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
