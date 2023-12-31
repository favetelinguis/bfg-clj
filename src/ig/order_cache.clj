(ns ig.order-cache
  (:require [core.events :as e]
            [cheshire.core :as json]
            [ig.cache :as cache]))

;; Order cache has the shape
;; {"<epic>" [market-state order-state {order-data}]}
;;
;; Makes sure only one order is avaliable/market
;; Confirms ONLY handle rejected orders after trying to open them
;; OPU used for all other changes to orders

(def order-none :order_none)
(def order-open-initiated :order_open_initiated)
(def order-close-initiated :order_close_initiated)
(def order-failure :order_failure)
(def order-success :order_success)

(def market-open :market_open)
(def market-closed :market_closed)

(defn insert-entry
  [m key entry]
  (assoc m key entry))

(defn make-entry
  ([]
   (make-entry market-closed order-none {}))
  ([x y m]
   [x y m]))

(defn is-in-state?
  [n-fn state m epic]
  (-> m
      (get epic)
      n-fn
      (= state)))

(def order-is-in-state? (partial is-in-state? second))
(def market-is-in-state? (partial is-in-state? first))

(def order-none? (partial order-is-in-state? order-none))
(def order-open-initiated? (partial order-is-in-state? order-open-initiated))
(def order-close-initiated? (partial order-is-in-state? order-close-initiated))
(def order-failure? (partial order-is-in-state? order-failure))
(def order-success? (partial order-is-in-state? order-success))

(def market-open? (partial market-is-in-state? market-open))

(defn get-in-order
  [m s key]
  (get-in m [s 2 ::e/data key]))

(defn change-order-state
  "Update order state, if key dont exist in map return old map"
  [s m key & [event]]
  (if (get m key)
    (if event
      (update m key (fn [[market _ _]] (make-entry market s event)))
      (update m key (fn [[market _ e]] (make-entry market s e))))
    m))

(def set-order-open-initiated (partial change-order-state order-open-initiated))
(def set-order-close-initiated (partial change-order-state order-close-initiated))
(def set-order-failure (partial change-order-state order-failure))
(def set-order-success (partial change-order-state order-success))

(defn change-market-state
  ""
  [s m key]
  (if (get m key)
    (update m key (fn [[_ order data]] (make-entry s order data)))
    m))

(def set-market-open (partial change-market-state market-open))
(def set-market-closed (partial change-market-state market-closed))

(defn get-balance
  [m]
  (::e/balance m))

(defn position-size [] 1)

(defn update-confirms
  "dealStatus ACCEPTED|REJECTED"
  [events+cache change]
  (let [[_ old] events+cache
        epic (:epic change)
        status (:dealStatus change)
        order-pending? (or (order-open-initiated? old epic)
                           (order-close-initiated? old epic))]
    (if order-pending? ;; Only care about confirms if we have an order pending
      (if (= status "REJECTED")
        (cache/make [(e/panic {::e/reason (str "String Confirms rejected for " epic " since " (:reason change))})]
                    (set-order-failure old epic))
        (cache/make (set-order-success old epic)))
      (cache/make old))))

(defn- update-opu
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn- update-wou
  "Do nothing atm"
  [events+cache change]
  events+cache)

(defn- update-account
  "Do nothing atm"
  [[_ old] change]
  (let [balance (some-> change
                        (get "AVAILABLE_CASH")
                        ((fn [s]
                           (try (Double/parseDouble s)
                                (catch Exception _)))))]
    (if balance
      (->> balance
           (assoc old ::e/balance)
           cache/make)
      (cache/make old))))

(defn- update-signal
  "Only one order can exist for a market at a time."
  [[_ old] {:keys [::e/name ::e/direction]}]
  (if (market-open? old name)
    (if (order-none? old name)
      ;; If no order exist for market open order.
      (let [event [(e/update-order {::e/name name
                                    ::e/size (position-size)
                                    ::e/direction direction})]
            entry (set-order-open-initiated old name event)
            new (insert-entry old name entry)]
        (cache/make event new))
      (let [prev-direction (get-in-order old name ::e/direction)
            prev-size (get-in-order old name ::e/size)]
        (if (or (= direction prev-direction) (order-close-initiated? old name))
          ;; If an order in the same direction exist do nothing and its not already closing.
          (cache/make old)
          ;; If an order in other direction exist genereate event to close.
          (let [event [(e/update-order {::e/name name
                                        ::e/size prev-size ; must use size of last order to close it when using netting on ig
                                        ::e/direction direction})]
                entry (set-order-close-initiated old name event)
                new (insert-entry old name entry)]
            (cache/make event new)))))
    ;; if market is closed do nothing with signal and just return old
    (cache/make old)))

(defn update-market
  "Update the market if its tradeable or not"
  [[_ old] change]
  (let [epic (::e/name change)
        is-tradable? (= "TRADEABLE" (get change "MARKET_STATE"))
        is-delay? (= "1" (get change "MARKET_DELAY"))]
    (if (or is-delay? is-tradable?)
      (cache/make (set-market-closed old epic))
      (cache/make (set-market-open old epic)))))

(defn update-unsubscribe
  "if there are any open orders close them and remove from cache
  if there are no open orders just remove from cache."
  [[_ old] change]
  ;; TODO
  (cache/make old))

(defn update-subscribe
  "Create entry when we subscribe to a market, it will owerwrite if there already is an entry."
  [[_ old] {:keys [::e/name ::e/route]}]
  (if (= "MARKET" route)
    (cache/make (insert-entry old name (make-entry)))
    (cache/make old)))

(defn- update-order-failure
  "For now handle any order failures as panics"
  [[_ old] {:keys [::e/name]}]
  (cache/make [(e/panic {::e/reason (str "Closing order failed for " name)})]
              (set-order-failure old name)))

(defn update-cache
  "Removes the envelope event"
  [prev {:keys [::e/action ::e/data] :as event}]
  (case action
    "TRADE" (do
              (when-let [s (get data "CONFIRMS")]
                (update-confirms prev (json/decode s true)))
              (when-let [s (get data "OPU")]
                (update-opu prev (json/decode s true)))
              (when-let [s (get data "WOU")]
                (update-wou prev (json/decode s true))))
    "ACCOUNT" (update-account prev data)
    "SIGNAL" (update-signal prev data)
    "MARKET" (update-market prev data)
    "SUBSCRIBE" (update-subscribe prev data)
    "UNSUBSCRIBE" (update-unsubscribe prev data)
    "ORDER-FAILURE" (update-order-failure prev data)
    (println "Unsupported event in order-cache: " event)))
