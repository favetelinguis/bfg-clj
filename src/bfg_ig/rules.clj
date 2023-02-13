(ns bfg-ig.rules
  (:require
    [bfg.indicators.time-series :as ts]
    [bfg.portfolio.order :as order]
    [bfg.portfolio.position :as position]
    [bfg.signal :as signal]
    [bfg.indicators.atr-series :as atr]
    [bfg.indicators.ohlc-series :as ohlc]
    [bfg.indicators.heikin-ashi-series :as ha]
    [odoyle.rules :as o]))

(def rule-set
  "
  Bar -> OHLC -> ATR -> HEIKIN-ASHI -> SignalCheck Update order
  This ensures that heikin-ashi series is the same lenght as atr else we could try
  to get atr at fiest heikin-ashi bar but that atr bar do not exist
  "
  (o/ruleset
    {
     ::update-ohlc-series
     [:what
      [market-id ::ohlc/bar ohlc-bar]
      [market-id ::ohlc/series ohlc-series {:then false}]
      :then
      (o/insert! market-id ::ohlc/series (ohlc/add-ohlc-bar ohlc-series ohlc-bar))]

     ::initiate-atr
     [:what
      [market-id ::ohlc/series ohlc-series]
      :when
      (atr/init-atr? ohlc-series)
      :then
      (o/insert! market-id ::atr/series (atr/make-atr-series ohlc-series))]

     ::update-atr-series
     [:what
      [market-id ::ohlc/series ohlc-series]
      [market-id ::atr/series atr-series {:then false}]
      :when
      (atr/after-atr-init? ohlc-series)
      :then
      (o/insert! market-id ::atr/series
                 (atr/add-atr-bar atr-series
                                  (ts/get-first ohlc-series)
                                  (ts/get-second ohlc-series)))]

     ::update-ha-series
     [:what
      [market-id ::atr/series trigger]
      [market-id ::ha/series ha-series {:then false}]
      [market-id ::ohlc/series ohlc-series {:then false}]
      :then
      (o/insert! market-id ::ha/series (ha/add-heikin-ashi-bar ha-series (ts/get-first ohlc-series)))]

     ::setup->entry
     [:what
      [market-id ::signal/signal :await-setup]
      [market-id ::ohlc/series ohlc-series {:then false}]
      [market-id ::atr/series atr-series {:then false}]
      [market-id ::ha/series ha-series]
      :when
      (signal/setup? ohlc-series atr-series ha-series)
      :then
      (o/insert! market-id ::signal/signal :await-entry)]

     ::entry->wo-confirmation
     [:what
      [market-id ::signal/signal :await-entry]
      [market-id ::ha/series ha-series]
      :when
      (signal/entry? ha-series)
      :then
      (o/insert! market-id ::signal/signal :await-wo-confirmation)
      (println "DO PLACE WO execute command ")
      ]

     ::entry->setup
     [:what
      [market-id ::signal/signal :await-entry]
      [market-id ::ha/series ha-series]
      :when
      (signal/cancel-entry? ha-series)
      :then
      (o/insert! market-id ::signal/signal :await-setup)
      ]

     ::wo-confirmation->exit
     [:what
      [market-id ::signal/signal :await-wo-confirmation]
      [market-id ::order/order order]
      :when
      (signal/wo-confirmation? order)
      :then
      (o/insert! market-id ::signal/signal :await-exit)
      ]

     ::close-wo->setup
     [:what
      [market-id ::signal/signal :await-exit]
      [market-id ::order/order order]
      [market-id ::ha/series ha-series]
      :when
      (signal/close-wo? order ha-series)
      :then
      (o/retract! market-id ::order/order)
      (o/insert! market-id ::signal/signal :await-setup)
      ]

     ::exit-position->setup
     [:what
      [market-id ::signal/signal :await-exit]
      [market-id ::position/position position]
      [market-id ::ha/series ha-series]
      :when
      (signal/exit-position? position ha-series)
      :then
      (o/retract! market-id ::position/position)
      (o/insert! market-id ::signal/signal :await-setup)
      ]

     ::signal
     [:what
      [market-id ::signal/signal signal]]

     ::ohlc-bar
     [:what
      [market-id ::ohlc/bar ohlc-bar]]

     ::ohlc-series
     [:what
      [market-id ::ohlc/series ohlc-series]]

     ::atr-series
     [:what
      [market-id ::atr/series atr-series]]

     ::ha-series
     [:what
      [market-id ::ha/series ha-series]]
     ;::ha
     ;[:what
     ; [market-id ::ha/state ha]]
     }))

(defn init-bfg-session
  [rules]
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert :DAX ::signal/signal :await-setup)
      (o/insert :DAX ::ha/series (ha/make-empty-series))
      (o/insert :DAX ::ohlc/series (ohlc/make-empty-series))))
;(def *session
;  (atom (reduce o/add-rule (o/->session) rules)))
;
;(defn run []
;  (swap! *session
;         (fn [session]
;           (-> session
;               (o/insert "DAX" ::atr/atr-state (atr/make-atr-state data/dax-bar-series-14))
;               (o/insert "DAX" ::ha/state (ha/make-heikin-ashi-series data/dax-bar-series-14))
;               (o/insert "DAX" ::bar/bar data/bar-0)
;               o/fire-rules
;               (o/insert "DAX" ::bar/bar data/bar-1)
;               o/fire-rules)))
;  :done)
;(run)
;
;(o/query-all @*session ::ha)
