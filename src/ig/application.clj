(ns ig.application
  (:require [clojure.core.async :as a]
            [ig.market-cache :as market-cache]
            [ig.cache :as cache]
            [ig.account-cache :as account-cache]
            [ig.stream.item :as i]
            [core.events :as e]))

(defn go-token-adder
  [c kill-switch]
  (a/go-loop []
    (let [[_ trigger-chan] (a/alts! [kill-switch (a/timeout 1000)] :priority true)]
      (when (= trigger-chan c)
        (a/>! c :token)
        (recur)))))

(defn go-command-executor
  [f ->in out-> bucket-chan kill-switch]
  (a/go-loop []
    (let [[c x] (a/alts! [kill-switch ->in])]
      (when (= c ->in)
        (a/<! bucket-chan) ; ratelimit, will block until we have a token
        (f out-> x)
        (recur)))))

(defn app
  ""
  ([f] (app f nil))
  ([f debug-fn]
   (let [kill-switch (a/chan 1)
         ig-rate-limit 30 ; 30/min can be higher for trade events
         token-bucket-chan (a/chan (a/dropping-buffer ig-rate-limit))
         stream-chan-> (a/chan 1)
         stream-topic (a/pub stream-chan-> #(i/get-route (get % "ROUTE")))
         ->portfolio-chan (a/chan 1)
         portfolio-mix (a/mix ->portfolio-chan)
         portfolio-chan-> (a/chan 1)
         ->instrument-chan (a/chan 1)
         instrument-chan-> (a/chan 1)
         market-topic (a/pub instrument-chan-> ::e/name)
         ->account-chan (a/chan 1)
         account-chan-> (a/chan 1)
         ->order-chan (a/chan 1)
         order-mix (a/mix ->order-chan)
         order-chan-> (a/chan 1)
         order-topic (a/pub order-chan-> ::e/kind)
         ->command-executor-chan (a/chan 1)
         command-executor-chan-> (a/chan 1)
         go-make-handler (fn
                           [->in out-> f m]
                           (fn []
                             (a/go-loop [prev-state m]
                               (let [[in-event c] (a/alts! [->in kill-switch])]
                                 (when debug-fn
                                   (if (= kill-switch c)
                                     (debug-fn :killed)
                                     (debug-fn in-event)))
                                 (when in-event
                                   (let [[out-events next-state] (f [[] prev-state] in-event)]
                                     (doseq [e out-events]
                                       (a/>! out-> e))
                                     (recur next-state)))))))
         go-instrument-store (go-make-handler ->instrument-chan instrument-chan-> market-cache/update-cache (cache/make))
         go-account-store (go-make-handler ->account-chan account-chan-> account-cache/update-cache (cache/make))
         go-order-store (go-make-handler ->order-chan order-chan-> market-cache/update-cache (cache/make))
         go-portfolio (go-make-handler ->portfolio-chan portfolio-chan-> market-cache/update-cache (cache/make))]
     ;; Instrument need
     (a/sub stream-topic "UNSUBSCRIBE" ->instrument-chan)
     (a/sub stream-topic "MARKET" ->instrument-chan)
     (a/sub stream-topic "CHART" ->instrument-chan)
     ;; Account need
     (a/sub stream-topic "ACCOUNT" ->account-chan)
     ;; Order need
     (a/sub stream-topic "TRADE" ->order-chan)
     (a/admix order-mix portfolio-chan->)
     (a/admix order-mix command-executor-chan->)
     ;; Portfolio need + all strategies
     (a/admix portfolio-mix account-chan->)
     (a/sub order-topic ::e/exit ->portfolio-chan)
     ;; Command executor need
     (a/sub order-topic ::e/order-new ->command-executor-chan)

     ;; Start services
     (go-token-adder token-bucket-chan kill-switch)
     (go-instrument-store)
     (go-account-store)
     (go-order-store)
     (go-portfolio)
     (go-command-executor f ->command-executor-chan command-executor-chan-> token-bucket-chan kill-switch)

     ;; Return
     {:make-strategy (fn [f m s] (let [->strategy-chan (a/chan 1)
                                       strategy-chan-> (a/chan (a/sliding-buffer 1)) ; sliding-buffer since we only care about the latest signal
                                       ]
                                   ;; Strategy takes from
                                   (a/sub market-topic s ->strategy-chan)
                                   ;; Strategy send to
                                   (a/admix portfolio-mix strategy-chan->)
                                   ;; Start strategy
                                   (go-make-handler ->strategy-chan strategy-chan-> f m)))
      :kill-app (fn [] (a/close! kill-switch))
      :send-to-app!! (fn [event] (a/>!! stream-chan-> event))})))
