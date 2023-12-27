(ns ig.application
  (:require [clojure.core.async :as a]
            [ig.market-cache :as market-cache]
            [ig.order-cache :as order-cache]
            [ig.portfolio :as portfolio]
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
  ""
  [f ->in out-> bucket-chan kill-switch])

(defn start-app
  "f is the command executor function
  debug-fn gets all output events in system and runs in its own thread"
  ([f] (start-app f nil))
  ([f debug-fn]
   (let [kill-switch (a/chan 1)
         ->debug-chan (a/chan (a/dropping-buffer 500))
         ig-rate-limit 30 ; 30/min can be higher for trade events
         token-bucket-chan (a/chan (a/dropping-buffer ig-rate-limit))
         stream-chan-> (a/chan 1) ; TODO add transducer to infere correct event
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
                               (let [[in-event _] (a/alts! [->in kill-switch])]
                                 (when in-event
                                   (let [events+next-state (f prev-state in-event)
                                         [out-events _] events+next-state]
                                     (doseq [e out-events]
                                       (a/>! out-> e)
                                       (when debug-fn
                                         (a/>! ->debug-chan e)))
                                     (recur events+next-state)))))))
         go-instrument-store (go-make-handler ->instrument-chan instrument-chan-> market-cache/update-cache (cache/make))
         go-account-store (go-make-handler ->account-chan account-chan-> account-cache/update-cache (cache/make))
         go-order-store (go-make-handler ->order-chan order-chan-> order-cache/update-cache (cache/make))
         go-portfolio (go-make-handler ->portfolio-chan portfolio-chan-> portfolio/update-cache (cache/make))]
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

     ;; Command executor
     (a/go-loop []
       (let [[c x] (a/alts! [kill-switch ->command-executor-chan])
             failure-fn (fn [] (let [e (e/exit "SOME EPIC")]
                                 (a/>! command-executor-chan-> e)
                                 (when debug-fn
                                   (a/>! ->debug-chan e))))]
         (when (= c ->command-executor-chan)
           (a/<! token-bucket-chan) ; ratelimit, will block until we have a token
           (f failure-fn x) ; TODO need to get epic name and http-client fn will have to change
           (recur))))
     (go-command-executor f ->command-executor-chan command-executor-chan-> token-bucket-chan kill-switch)

     ;; Setup debug-fn if needed, dont read from original chanel when using mult
     (when debug-fn
       (a/sub stream-topic "UNSUBSCRIBE" ->debug-chan)
       (a/sub stream-topic "MARKET" ->debug-chan)
       (a/sub stream-topic "CHART" ->debug-chan)
       (a/sub stream-topic "ACCOUNT" ->debug-chan)
       (a/sub stream-topic "TRADE" ->debug-chan)
       (a/thread
         (loop []
           (let [[in-event _] (a/alts!! [->debug-chan kill-switch])]
             (when in-event
               (debug-fn in-event)
               (recur))))))

     ;; Return
     {:make-strategy (fn [f m s] (let [->strategy-chan (a/chan 1)
                                       strategy-chan-> (a/chan (a/sliding-buffer 1)) ; sliding-buffer since we only care about the latest signal
                                       ]
                                   ;; Strategy takes from
                                   (a/sub market-topic s ->strategy-chan)
                                   ;; Strategy send to
                                   (a/admix portfolio-mix strategy-chan->)
                                   ;; Start strategy
                                   (go-make-handler ->strategy-chan strategy-chan-> f m)
                                   ;; Return for easy close
                                   ->strategy-chan))
      :kill-app (fn [] (a/close! kill-switch))
      :send-to-app!! (fn [event] (a/>!! stream-chan-> event))})))

(defn start-appv2
  "f is the command executor function
  debug-fn gets all output events in system and runs in its own thread"
  ([f] (start-appv2 f println))
  ([f debug-fn]
   (let [kill-switch (a/chan 1)
         ig-rate-limit 30 ; 30/min can be higher for trade events
         token-bucket-chan (a/chan (a/dropping-buffer ig-rate-limit))
         event-bus-> (a/chan 1) ; Will be out-chan of all command handler
         ->event-sourcing-chan (a/chan (a/dropping-buffer 500)) ; make sure it do not block
         event-topic-chan (a/chan 1)
         event-bus-mult (a/mult event-bus->)
         _ (a/tap event-bus-mult ->event-sourcing-chan)
         _ (a/tap event-bus-mult event-topic-chan)
         event-topic (a/pub event-topic-chan ::e/action)

         ->portfolio-chan (a/chan 1)
         ->instrument-chan (a/chan 1)
         ->account-chan (a/chan 1)
         ->order-chan (a/chan 1)
         ->command-executor-chan (a/chan 1)
         go-make-handler (fn
                           [->in f m]
                           (fn []
                             (a/go-loop [prev-state m]
                               (let [[in-event _] (a/alts! [->in kill-switch])]
                                 (when in-event
                                   (let [events+next-state (f prev-state in-event)
                                         [out-events _] events+next-state]
                                     (doseq [e out-events]
                                       (a/>! event-bus-> e))
                                     (recur events+next-state)))))))

         go-instrument-store (go-make-handler ->instrument-chan  market-cache/update-cache (cache/make))
         go-account-store (go-make-handler ->account-chan  account-cache/update-cache (cache/make))
         go-order-store (go-make-handler ->order-chan  order-cache/update-cache (cache/make))
         go-portfolio (go-make-handler ->portfolio-chan  portfolio/update-cache (cache/make))]
     ;; Market need
     (a/sub event-topic ::e/chart ->instrument-chan)
     (a/sub event-topic ::e/market ->instrument-chan)
     (a/sub event-topic ::e/unsubscribe ->instrument-chan)
     ;; Account need
     (a/sub event-topic ::e/account ->account-chan)
     ;; Order need
     (a/sub event-topic ::e/trade ->order-chan)
     (a/sub event-topic ::e/make-order ->order-chan)
     (a/sub event-topic ::e/exit ->order-chan)
     ;; Portfolio need + all strategies
     (a/sub event-topic ::e/signal ->portfolio-chan)
     (a/sub event-topic ::e/balance ->portfolio-chan)
     (a/sub event-topic ::e/exit ->portfolio-chan)
     (a/sub event-topic ::e/filled ->portfolio-chan)
     ;; Command executor need
     (a/sub event-topic ::e/make-order ->command-executor-chan)

     ;; Start services
     (go-token-adder token-bucket-chan kill-switch)
     (go-instrument-store)
     (go-account-store)
     (go-order-store)
     (go-portfolio)

     ;; Command executor
     (a/go-loop []
       (let [[c x] (a/alts! [kill-switch ->command-executor-chan])
             failure-fn (fn [] (let [e (e/exit "SOME EPIC")]
                                 (a/>! event-bus-> e)))]
         (when (= c ->command-executor-chan)
           (a/<! token-bucket-chan) ; ratelimit, will block until we have a token
           (f failure-fn x) ; TODO need to get epic name and http-client fn will have to change
           (recur))))

     ;; Event source
     (a/thread
       (loop []
         (let [[in-event _] (a/alts!! [->event-sourcing-chan kill-switch])]
           (when in-event
             (debug-fn in-event)
             (recur)))))

     ;; Return
     {:make-strategy (fn [f m s] (let [->strategy-chan (a/chan 1)]
                                   ;; Strategy takes from
                                   (a/sub event-topic s ->strategy-chan)
                                   ;; Start strategy
                                   (go-make-handler ->strategy-chan f m)
                                   ;; Return for easy close
                                   ->strategy-chan))
      :kill-app (fn [] (a/close! kill-switch))
      :send-to-app!! (fn [event] (a/>!! event-bus-> event))})))
