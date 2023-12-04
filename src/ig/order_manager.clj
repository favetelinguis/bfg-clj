(ns ig.order-manager
  (:require [core.events :as e]
            [ig.order-manager.order-cache :as cache]
            [clojure.core.async :as a]
            [ig.rest :as rest]))

(defn start
  [rx portfolio-fn order-state http-client]
  (println "Starting OrderManager")
  (a/thread
    (try
      (loop []
        (when-let [{:keys [::e/kind] :as event} (a/<!! rx)]
          (case kind
            ; order-new from portfolio
            ::e/order-new (when-not (cache/has-order? @order-state (::e/name event))
                            (swap! order-state cache/add-order event) ; update order cache
                            (http-client (let [{:keys [::e/direction ::e/size ::e/name]} event] ; make http call delete order if call fails
                                           (rest/open-order name direction size "EUR"))
                                         :error-callback #(swap! order-state cache/remove-order name)))
            ;confirms send rejected delete ok send order event to portfolio
            :confirm (let [{:keys [epic dealStatus reason direction size]} event]
                       (when (cache/has-order? @order-state epic)
                         (case dealStatus
                           "REJECTED" (do
                                        (swap! order-state cache/remove-order epic)
                                        (println "Order rejected with reason " reason))
                           "ACCEPTED" (portfolio-fn (e/position-new epic (keyword direction) size)))))
            ;opu
            :opu (identity "NOT SUPPORTED")
            ;wou
            :wou (identity "NOT SUPPORTED")
            (println "Unsupported:  " event))
          (recur)))
      (println "Shutting down OrderManager")
      (catch Throwable e (println "Error in OrderManager: " (.getMessage e))))))
