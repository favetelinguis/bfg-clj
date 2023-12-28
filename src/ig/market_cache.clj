(ns ig.market-cache
  (:require [core.events :as e]
            [ig.cache :as cache]
            [ig.stream.item :as i])
  (:import [java.time Instant]))

;; This cache is part of the stateful transducers it must have the function signature
;; (fn [old-cache event] [[::e/events to propagate] updated-cache])

(defn update-status
  "This never produce events, it only provide metadata about the market, should be used
  to check if data is delayed etc"
  [[_ market-cache] change]
  (let [epic (::e/name change)]
    (cache/make (update market-cache epic merge change))))

(defn update-candle
  "If bid/offer_close changes send out new MidPrice event
  If cons_end = 1 send out new candle"
  [[_ market-cache] change]
  (let [complete-candle? (= (get change "CONS_END") "1")
        mid-price-change? (or (get change "OFR_CLOSE") (get change "BID_CLOSE"))
        epic (::e/name change)
        new-market-cache (update market-cache epic merge change)
        calculate-mid-price (fn [ofr bid]
                              (let [x (Double/parseDouble (get-in new-market-cache [epic ofr]))
                                    y (Double/parseDouble (get-in new-market-cache [epic bid]))]
                                (/ (+ x y) 2)))
        mid-price-update (when mid-price-change?
                           {::e/name epic ::e/price (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")})
        candle-update (when complete-candle?
                        {::e/name epic
                         ::e/time
                         (-> (get-in new-market-cache [epic "UTM"])
                             (Long/parseLong)
                             (Instant/ofEpochMilli))
                         ::e/high (calculate-mid-price "OFR_HIGH" "BID_HIGH")
                         ::e/low (calculate-mid-price "OFR_LOW" "BID_LOW")
                         ::e/open (calculate-mid-price "OFR_OPEN" "BID_OPEN")
                         ::e/close (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")})
        event (if (or mid-price-change? complete-candle?)
                [(e/signal-update epic (merge candle-update mid-price-update))]
                [])]
    (cache/make event new-market-cache)))

(defn remove-epic
  [[_ m] change]
  (let [epic (::e/name change)]
    (cache/make (dissoc m epic))))

(defn update-cache
  [prev {:keys [::e/action ::e/data] :as event}]
  (case action
    "MARKET" (update-status prev data)
    "CHART" (update-candle prev data)
    "UNSUBSCRIBE" (remove-epic prev data)
    ;; "SUBSCRIBE" (update-status prev data)
    (println "Unsupported event in market-cache: " event)))
