(ns bfg.bfg_context
  (:import (java.time Duration ZonedDateTime)))

(defn make-bar [market-id time open high low close volume]
  {:id market-id :time time :open open :high high :low low :close close :volume volume})

(defn make-bar-series [market-id duration]
  {:id market-id :bars (list) :duration duration}
  )

(defn add-bar
  "Check that each bar that is added in front is newer then the old bar"
  [bar-series bar]
  (let [is-same-market (= (:id bar) (:id (first (:bars bar-series))))
        is-empty-bar-series (empty? (:bars bar-series))
        is-new-bar-oldest (if is-empty-bar-series false (> (.compareTo (:time bar) (:time (first (:bars bar-series)))) 0))]
    (when (and is-same-market
            (or is-empty-bar-series is-new-bar-oldest))
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

(defn make-decision
  [id buy sell]
  {:id id :buy buy :sell sell})

(defn make-strategy
  [name]
  {:id name
   :buy-signal '()
   :sell-signal '()})

(defn run-strategy
  "Return a decision"
  [bar-series price-update strategy]
  (let [run-fn (fn [signal-key direction]
                 (some (partial = :BUY)
                       (apply (juxt (:buy-signal strategy)) [bar-series price-update])))
        buy (run-fn :buy-signal :BUY)
        sell (run-fn :sell-signal :SELL)]
    (make-decision (:id strategy) buy sell)))

(defn stupid-strategy-1
  [direction]
  (fn [bar-series price-update]
    (let [close-fn (last-price :close)
          sa-fn (sa 2 :close)]
      (when (> (close-fn bar-series) (sa-fn bar-series)
               direction)))))

(defn stupid-strategy-2
  [direction]
  (fn [bar-series price-update]
    (let [close-fn (last-price :close)
          sa-fn (sa 2 :close)]
      (when (< (close-fn bar-series) (sa-fn bar-series)
               direction)))))

(defn example-strategy
  "Takes a bar series and return a signal"
  []
 (-> (make-strategy "Example strategy")
     (update :buy-signal conj (stupid-strategy-1 :BUY))
     (update :sell-signal conj (stupid-strategy-2 :SELL))))

(defn make-account
  [market-id total available]
  {:id market-id :total total :available available})

(defn make-market
  [market-id strategies bar-series current-price]
  {:id market-id :strategies strategies :bar-series bar-series :current-price current-price})

(defn make-context
  []
  {:markets {}
   :accounts {}
   :portfolio nil
   :decisions nil
   }
  )

(defn add-to-context [key context m]
  (update context key assoc (:id m) m))
(def add-market (partial add-to-context :markets))
(def add-account (partial add-to-context :accounts))

(defn update-account
  [old-account update]
  (merge old-account update))

(defn update-market
  [old-account update]
  (merge old-account update))

(defn update-decisions
  ""
  [context price-update]
  (let [market-bar-series (get-in context [:bar-series (:id price-update)])
        market-strategies (get-in context [:market (:id price-update) :strategies])]
    (assoc context :decisions (map
                              (partial run-strategy market-bar-series price-update) market-strategies))))

(defmulti update :type)
(defmethod update :account [update last-context]
  (update-in last-context [:accounts (:id update)] update-account update))
(defmethod update :price [update last-context]
  (->
    (update-in last-context [:markets (:id update)] update-market update)
    (update-decisions update)))
(defmethod update :bar [update last-context]
  (update-in last-context [:markets (:id update) :bar-series] add-bar update))
(defmethod update :default [_ _] nil)

(defn example-position-sizing-strategy
  "TODO implement custom position sizing strategies"
  [account decisions])
;; Pure BFG-CORE
;; Pure BFG-IG Handle Order FSM Market FSM
;; Impure BFG-IG Lighstreamer connection
;; Main metod IMPL
;; Impure
;;; Placed in non pure core
;(def context (atom {:context {} :decisions []}))
;(defn run!
;  [market-update]
;  (let [{:keys [context decisions account]} (swap! context (partial update market-update))]
;    (->> decisions
;         (position-sizing account)
;         ;execute-commands
;         )))