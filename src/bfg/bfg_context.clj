(ns bfg.bfg_context
  (:require [clojure.spec.alpha :as s])
  (:import (java.time Duration ZonedDateTime)))

;; SPECS
(s/def ::signal #{:BUY :SELL nil})

;;; END SPECS

(defn make-bar [market-id time open high low close volume]
  {:id market-id :time time :open open :high high :low low :close close :volume volume})

(defn make-bar-series [market-id duration]
  {:id market-id :bars (list) :duration duration}
  )

(defn add-bar
  "Check that each bar that is added in front is newer then the old bar
  This is very sensetive, if we update with a faulty value the barseries in a market will become nil and the
  trading system should stop  rading!
  "
  [bar-series bar]
  (let [is-empty-bar-series (empty? (:bars bar-series))
        is-same-market (= (:id bar) (:id (first (:bars bar-series))))
        is-new-bar-oldest (if is-empty-bar-series false (> (.compareTo (:time bar) (:time (first (:bars bar-series)))) 0))]
    (when (or is-empty-bar-series
              (and is-same-market is-new-bar-oldest))
      (update bar-series :bars conj bar))))

(defn add-bars
  [bar-series & bars]
  (reduce add-bar bar-series bars))

;; put in indicator ns
(defn last-price
  "Return last close price or nil"
  [price-key]
  (fn [bar-series]
    (price-key (first (:bars bar-series)))))

(defn sa
  "Return the average price during periods for price or nil if no enough data"
  [periods price-key]
  (fn [bar-series]
    (when (>= (count (:bars bar-series)) periods)
      (reduce + 0 (map price-key (take periods (:bars bar-series)))))))

(defn make-signal
  [market-id strategy-name buy sell]
  {:market-id market-id :strategy-name strategy-name :buy buy :sell sell})

(defn make-strategy
  [name]
  {:id          name
   :buy-signal  '()
   :sell-signal '()})

(defn run-strategy
  "Return a decision"
  [bar-series price-update strategy]
  (let [run-fn (fn [signal-key direction]
                 (some (partial = direction)
                       ((apply juxt (signal-key strategy)) bar-series price-update)))
        buy (run-fn :buy-signal :BUY)
        sell (run-fn :sell-signal :SELL)]
    (make-signal (:id price-update) (:id strategy) buy sell)))

(defn stupid-strategy-1
  [direction]
  (fn [bar-series price-update]
    (let [close-fn (last-price :close)
          sa-fn (sa 2 :close)]
      (when (> (close-fn bar-series) (sa-fn bar-series))
        direction))))
;; TODO how to view this in cursive?
;; Should return a function and that function returns a signal
(s/fdef stupid-strategy-1
        ;:args (s/cat :account-id ::id)
        :ret ::signal)

(defn stupid-strategy-2
  "Return signal"
  [direction]
  (fn [bar-series price-update]
    (let [close-fn (last-price :close)
          sa-fn (sa 2 :close)]
      (when (< (close-fn bar-series) (sa-fn bar-series))
        direction))))

(defn make-account
  [market-id total available currency]
  {:id market-id :total total :available available})

(defn make-market
  "TODO make sure args cant be nil"
  [market-id strategies bar-series current-price currency]
  {:id            market-id
   :strategies    strategies
   :bar-series    bar-series
   :current-price current-price
   })

(defn make-context
  [position-sizing-strategy]
  {:markets                  {}
   :accounts                 {}
   :portfolio                (bfg.portfolio/make-portfolio)
   :position-sizing-strategy position-sizing-strategy
   :signals                nil
   :delta-portfolio-commands nil
   }
  )

(defn update-account
  [old-account update]
  (merge old-account update))

(defn update-market
  [old-account update]
  (merge old-account update))

(defn update-signals
  ""
  [context price-update]
  (let [market-bar-series (get-in context [:markets (:id price-update) :bar-series])
        market-strategies (get-in context [:markets (:id price-update) :strategies])]
    (assoc context :signals (map
                                (partial run-strategy market-bar-series price-update) market-strategies))))

(defn update-delta-portfolio-commands
  [context]
  (assoc context :delta-portfolio-commands (apply
                                             (:position-sizing-strategy context)
                                             ((juxt :portfolio :accounts :signals) context))))

(defmulti update (fn [last-context [action update]] action))
(defmethod update :market [last-context [_ update]]
  (update-in last-context [:markets (:id update)] update-market update))
(defmethod update :account [last-context [_ update]]
  (update-in last-context [:accounts (:id update)] update-account update))
;; TODO how to handle adding a bar if the market dont exist
(defmethod update :bar [last-context [_ bar-update]]
  (update-in last-context [:markets (:id bar-update) :bar-series] add-bar bar-update))
(defmethod update :price [last-context [_ price-update]]
  (->
    (update-in last-context [:markets (:id price-update)] update-market price-update)
    (update-signals price-update)
    update-delta-portfolio-commands))
(defmethod update :default [last-context _] last-context)

;; Pure BFG-CORE
;; Pure BFG-IG Handle Order FSM Market FSM
;; Impure BFG-IG Lighstreamer connection
;; Main metod IMPL
;(def context (atom {:context {} :decisions []}))
;(defn run!
;  [market-update]
;  (let [{:keys [context decisions account]} (swap! context (partial update market-update))]
;    (->> decisions
;         (position-sizing account)
;         ;execute-commands
;         )))