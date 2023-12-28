(ns ig.account-cache
  (:require [core.events :as e]
            [ig.stream.item :as i]
            [ig.cache :as cache]))

(defn update-account
  "Now I only have one value in account update so always send out update for each change"
  [[_ old] change]
  (let [account (i/get-name (get change "ROUTE"))
        balance (get change "AVAILABLE_CASH")]
    (cache/make
     (if balance [(e/balance {::e/name account ::e/balance balance})] [])
     (update old account merge change))))

(defn update-cache
  [prev event]
  (let [route (get event "ROUTE")]
    (cond
      (i/account? route) (update-account prev event)
      :else (println "Unsupported event account: " event))))
