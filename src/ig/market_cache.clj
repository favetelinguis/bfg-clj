(ns ig.market-cache
  (:require [core.events :as e]))

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn update-status
  "This never produce events, it only provide metadata about the market, should be used
  the check if data is delayed etc"
  [[_ market-cache] change]
  (let [epic (get change "NAME")]
    (make (update market-cache epic merge change))))

(defn update-candle
  "If bid/offer_close changes send out new MidPrice event
  If cons_end = 1 send out new candle"
  [[_ market-cache] change]
  (let [complete-candle? (= (get change "CONS_END") "1")
        mid-price-change? (or (get change "OFR_CLOSE") (get change "BID_CLOSE"))
        epic (get change "NAME")
        new-market-cache (update market-cache epic merge change)
        calculate-mid-price (fn [bid ofr]
                              (let [x (Double/parseDouble (get-in new-market-cache [epic ofr]))
                                    y (Double/parseDouble (get-in new-market-cache [epic bid]))]
                                (/ (+ x y) 2)))
        events (remove nil? [(when mid-price-change?
                               (e/create-mid-price-event epic (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")))

                             (when complete-candle?
                               (e/create-candle-event epic
                                                      (calculate-mid-price "OFR_OPEN" "BID_OPEN")
                                                      (calculate-mid-price "OFR_HIGH" "BID_HIGH")
                                                      (calculate-mid-price "OFR_LOW" "BID_LOW")
                                                      (calculate-mid-price "OFR_CLOSE" "BID_CLOSE")
                                                      (get-in new-market-cache [epic "UTM"])))])]
    (make events new-market-cache)))

(defn remove-epic
  [[_ m] epic]
  (make (dissoc m epic)))
