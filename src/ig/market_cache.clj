(ns ig.market-cache
  (:require [core.events :as e]))

(defn new []
  [[] {}])

(defn update-status
  "{UPDATE_TIME 20:27:18, MARKET_DELAY 0, MARKET_STATE TRADEABLE, BID 14794.2, OFFER 14797.0}
  nil keys in new-m are filterd out"
  [[_ market-cache] change]
  (let [bid (get change "BID")
        offer (get change "OFFER")
        epic (get change "NAME")]
    [(into [] (remove nil? [(when bid
                             (e/create-bid-event epic (Double/parseDouble bid)))
                           (when offer
                             (e/create-ask-event epic (Double/parseDouble offer)))]))
     (update market-cache epic merge change)]))

(defn remove-epic
  [[_ m] epic]
  [[] (dissoc m epic)])
