(ns core.portfolio.rules
  (:require
   [core.indicators.ohlc-series :as ohlc]
   [core.signal :as signal]
   [odoyle.rules :as o]
   [core.events :as e]
   [core.command :as command]))

(def rules
  (o/ruleset
   {::candle-update
    [:what
     [epic ::e/candle candle]
     [epic ::barseries bars {:then false}]
     :then
     (o/insert! epic ::barseries (ohlc/add-bar bars candle))]

    ::run-signal-on-midprice
    [:what
     [epic ::e/mid-price price]
     [epic ::signal-activated id {:then false}]
     [id ::signal data {:then false}]
     :then
     (let [new-signal (signal/on-midprice data price)]
       (o/insert! id ::signal new-signal)
       (when-let [command (signal/get-commands new-signal epic)]
         (o/insert! (::e/uuid command) command)))]

    ::run-signal-on-barseries-new-bar
    [:what
     [epic ::barseries bars]
     [epic ::signal-activated id {:then false}]
     [id ::signal data {:then false}]
     :then
     (let [new-signal (signal/on-candle data bars)]
       (o/insert! id ::signal new-signal)
       (when-let [command (signal/get-commands new-signal epic)]
         (o/insert! (::e/name command) (::e/kind command) command)))]

    ::execute-new-order
    [:what
     [epic ::e/order-new new-order]
     [::executors ::command c {:then false}]
     [epic ::e/balance balance {:then false}]
     :then
     (command/open-order! c new-order)
     (o/retract! epic ::e/order-new)]

    ::get-positions
    [:what
     [epic ::e/position-exit position]]

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
  [command-executor signals & {:keys [id-fn alt-rules] :or {id-fn #(str (java.util.UUID/randomUUID))
                                                            alt-rules rules}}]
  (-> (reduce o/add-rule (o/->session) alt-rules)
      (#(reduce
         (fn [session signal] (o/insert session (id-fn) ::signal signal))
         %
         signals)) ; Add in all strategies
      (o/insert ::executors ::command command-executor)
      o/fire-rules))

(defn get-positions
  [session]
  (o/query-all session ::get-positions))

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
  [session {:keys [::e/kind ::e/name ::e/account-id] :as event}]
  (let [to-insert (case kind
                    ::e/mid-price [name kind event]
                    ::e/candle [name kind event]
                    ::e/balance [account-id kind event]
                    ::e/position-new [name kind event]
                    ; TODO add all event kinds
                    [::event ::unknown event])]
    (-> session
        (o/insert to-insert)
        o/fire-rules)))

(defn activate-signal-for-market
  [session id epic]
  (-> session
      (o/insert epic ::signal-activated id)
      (o/fire-rules)))

(defn deactivate-signal-for-market
  [session id epic]
  (-> session
      (o/retract epic ::signal-activated)
      (o/fire-rules)))

(defn subscribe-market
  [session epic]
  (-> session
      (o/insert epic ::barseries (ohlc/make-empty-series))
      (o/fire-rules)))

(defn unsubscribe-market
  [session epic]
  (-> session
      (o/retract epic ::barseries)
      (o/fire-rules)))
