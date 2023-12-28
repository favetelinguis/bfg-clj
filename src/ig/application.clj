(ns ig.application
  (:require [clojure.core.async :as a]
            [ig.market-cache :as market-cache]
            [ig.order-cache :as order-cache]
            [ig.cache :as cache]
            [core.events :as e]))

(defn start-app
  "f is the command executor function
  debug-fn gets all output events in system and runs in its own thread"
  ([f] (start-app f println))
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

         ->instrument-chan (a/chan 1)
         ->order-chan (a/chan 1)
         ->command-executor-chan (a/chan 1)
         go-make-handler (fn
                           [->in f m]
                           (fn [pid]
                             (a/go-loop [prev-state m]
                               (let [[in-event _] (a/alts! [->in kill-switch])]
                                 (when in-event
                                   (let [events+next-state (try (f prev-state in-event)
                                                                (catch Throwable ex (do
                                                                                      (println "Exception in " pid " Message: " (ex-message ex))
                                                                                      ; TODO what more do i need to do, now I clear state, is that ok?
                                                                                      (cache/make [(e/panic ex)] {}))))
                                         [out-events _] events+next-state]
                                     (doseq [e out-events]
                                       (a/>! event-bus-> e))
                                     (recur events+next-state)))))))

         go-instrument-store (go-make-handler ->instrument-chan  market-cache/update-cache (cache/make))
         go-order-store (go-make-handler ->order-chan order-cache/update-cache (cache/make))]
     ;; Market need
     (a/sub event-topic "CHART" ->instrument-chan)
     (a/sub event-topic "MARKET" ->instrument-chan)
     (a/sub event-topic "UNSUBSCRIBE" ->instrument-chan)
     (a/sub event-topic "SUBSCRIBE" ->instrument-chan)
     ;; Order need
     (a/sub event-topic "TRADE" ->order-chan)
     (a/sub event-topic "ACCOUNT" ->order-chan)
     (a/sub event-topic "SIGNAL" ->order-chan)
     (a/sub event-topic "ORDER-CREATE-FAILURE" ->order-chan)
     ;; Command executor need
     (a/sub event-topic "OPEN-ORDER" ->command-executor-chan)

     ;; Start services
     (go-instrument-store "Instrument store")
     (go-order-store "Order store")

     ;; Token bucket adder for rate limiting
     (a/go-loop []
       (let [[_ trigger-chan] (a/alts! [kill-switch (a/timeout 1000)] :priority true)]
         (when-not (= trigger-chan kill-switch)
           (a/>! token-bucket-chan :token)
           (recur))))

     ;; Command executor
     (let [failure-fn (fn [epic] (let [e (e/order-create-failure epic)]
                                   (a/go (a/>! event-bus-> e))))]
       (a/go-loop []
         (let [[x _] (a/alts! [kill-switch ->command-executor-chan])]
           (when x
             (a/<! token-bucket-chan) ; ratelimit, will block until we have a token
             (try
               (f failure-fn x)
               (catch Throwable ex (println "Exception in Command executor Message: " (ex-message ex))))

             (recur)))))

     ;; Event source
     (a/thread
       (loop []
         (let [[in-event _] (a/alts!! [->event-sourcing-chan kill-switch])]
           (when in-event
             (try
               (debug-fn in-event)
               (catch Throwable ex (println "Exception in event source process Message: " (ex-message ex))))
             (recur)))))

     ;; Return
     {:make-strategy (fn [f m s] (let [->strategy-chan (a/chan 1)]
                                   ;; Strategy takes from
                                   (a/sub event-topic s ->strategy-chan)
                                   ;; Start strategy
                                   ((go-make-handler ->strategy-chan f m) (str "Strategy " s))
                                   ;; Return for easy close
                                   ->strategy-chan))
      :kill-app (fn [] (a/close! kill-switch))
      :send-to-app!! (fn [event] (a/>!! event-bus-> event))})))
