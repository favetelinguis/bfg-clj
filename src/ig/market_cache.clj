(ns ig.market-cache
  (:require [core.events :as e]
            [ig.stream.item :as i])
  (:import [java.time Instant]))

;; This cache is part of the stateful transducers it must have the function signature
;; (fn [old-cache event] [[::e/events to propagate] updated-cache])

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn update-status
  "This never produce events, it only provide metadata about the market, should be used
  to check if data is delayed etc"
  [[_ market-cache] change]
  (let [epic (i/get-name (get change "ROUTE"))]
    (make (update market-cache epic merge change))))

(defn update-candle
  "If bid/offer_close changes send out new MidPrice event
  If cons_end = 1 send out new candle"
  [[_ market-cache] change]
  (let [complete-candle? (= (get change "CONS_END") "1")
        mid-price-change? (or (get change "OFR_CLOSE") (get change "BID_CLOSE"))
        epic (i/get-name (get change "ROUTE"))
        new-market-cache (update market-cache epic merge change)
        calculate-mid-price (fn [bid ofr]
                              (let [x (Double/parseDouble (get-in new-market-cache [epic ofr]))
                                    y (Double/parseDouble (get-in new-market-cache [epic bid]))]
                                (/ (+ x y) 2)))
        events (remove nil? [(when mid-price-change?
                               (e/create-mid-price-event epic (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")))

                             (when complete-candle?
                               (e/create-candle-event epic
                                                      (-> (get-in new-market-cache [epic "UTM"])
                                                          (Long/parseLong)
                                                          (Instant/ofEpochMilli))
                                                      (calculate-mid-price "OFR_HIGH" "BID_HIGH")
                                                      (calculate-mid-price "OFR_LOW" "BID_LOW")
                                                      (calculate-mid-price "OFR_OPEN" "BID_OPEN")
                                                      (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")))])]
    (make events new-market-cache)))

(defn remove-epic
  [[_ m] change]
  (let [epic (i/get-name (get "ROUTE" change))]
    (make (dissoc m epic))))

(defn update-cache [old event]
  (let [route (get event "ROUTE")]
    (cond
      (i/market? route) (update-status old event)
      (i/chart? route) (update-candle old event)
      (i/unsubscribe? route) (remove-epic old event)
      :else (println "Unsupported event: " event))))
