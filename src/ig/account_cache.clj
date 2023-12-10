(ns ig.account-cache
  (:require [core.events :as e]
            [ig.stream.item :as i]))

;; This cache is part of the stateful transducers it must have the function signature
;; (fn [old-cache event] [[::e/events to propagate] updated-cache])

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn update-account
  "Now I only have one value in account update so always send out update for each change"
  [old change]
  (let [account (i/get-name (get change "ROUTE"))
        cache (get change "AVAILABLE_CASH")]
    (make
     [(e/create-balance-event account cache)]
     (update old account merge change))))

(defn update-cache [old event]
  (let [route (get event "ROUTE")]
    (cond
      (i/account? route) (update-account old event)
      :else (println "Unsupported event: " event))))
