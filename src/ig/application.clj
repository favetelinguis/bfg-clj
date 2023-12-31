(ns ig.application
  (:require [clojure.core.async :as a]
            [ig.market-cache :as market-cache]
            [ig.order-cache :as order-cache]
            [ig.cache :as cache]
            [core.events :as e]))

(def kill-switch (a/chan 1))

(def event-bus> (a/chan 1))

(def >event-source (a/chan (a/dropping-buffer 1024)))

(def event-topic
  (let [event-bus-mult (a/mult event-bus>)
        event-topic> (a/chan 1)]
    (a/tap event-bus-mult >event-source)
    (a/tap event-bus-mult event-topic>)
    (a/pub event-topic> ::e/action)))

(defn go-consumer
  "Calling without m will create a sink calling with m will produce a consumer/producer
  for a sink f need to be event->ignored
  for a consumer/producer f need to be [[prev-events] prev-state] -> [[next-events] next-state]"
  ([id f] (go-consumer id f nil))
  ([id f m]
   (fn [>in]
     (a/go-loop [prev-state m]
       (let [[in-event _] (a/alts! [>in kill-switch])]
         (when in-event
           (let [events+next-state (try (if m
                                          (f prev-state in-event)
                                          (f in-event))
                                        (catch Throwable ex (do
                                                              (println "Exception in " id " Message: " (ex-message ex))
                                                              ;; TODO what more do i need to do, now I clear state, is that ok?
                                                              ;; TODO a sink will not prouce panic events, do i want that?
                                                              (cache/make [(e/panic ex)] {}))))]
             (when m
               (let [[out-events _] events+next-state]
                 (doseq [e out-events]
                   (a/>! event-bus> e))))
             (recur events+next-state))))))))

(defn subscribe-consumer
  [consumer-fn publication & topics]
  (let [consumer-chan (a/chan)]
    (doseq [topic topics]
      (a/sub publication topic consumer-chan))
    (consumer-fn consumer-chan)
    consumer-chan))

(defn kill-app
  []
  (a/close! kill-switch))

(defn make-strategy
  "Returns a kill fn to close strategy"
  [f m s]
  (let [>in (subscribe-consumer (go-consumer (str "Strategy " s) f m)
                                event-topic
                                s)]
    (fn [] (a/close! >in))))

(defn send-to-app!!
  [event]
  (a/>!! event-bus> event))

(defn send-to-app!
  [event]
  (a/go (a/>! event-bus> event)))

(defn start-instrument-store []
  (subscribe-consumer (go-consumer "Instrument Store" market-cache/update-cache (cache/make))
                      event-topic
                      "CHART"
                      "UNSUBSCRIBE"
                      "SUBSCRIBE"))

(defn start-order-store []
  (subscribe-consumer (go-consumer "Order Store" order-cache/update-cache (cache/make))
                      event-topic
                      "MARKET"
                      "SUBSCRIBE"
                      "UNSUBSCRIBE"
                      "TRADE"
                      "ACCOUNT"
                      "SIGNAL"
                      "ORDER-FAILURE"))

(defn start-event-source [f]
  ((go-consumer "Event source" f) >event-source))

(defn start-order-executor [f]
  (let [ig-rate-limit 30 ; 30/min can be higher for trade events
        token-bucket-chan (a/chan (a/dropping-buffer ig-rate-limit))
        executor-fn (fn [>in]
                      (a/go-loop []
                        (let [[x _] (a/alts! [kill-switch >in])]
                          (when x
                            (a/<! token-bucket-chan) ; ratelimit, will block until we have a token
                            (try
                              (f x)
                              (catch Throwable ex (println "Exception in Command executor Message: " (ex-message ex))))
                            (recur)))))]
    ;; start token added which is used as a rate limiter for order executor
    (a/go-loop []
      (let [[_ trigger-chan] (a/alts! [kill-switch (a/timeout 1000)] :priority true)]
        (when-not (= trigger-chan kill-switch)
          (a/>! token-bucket-chan :token)
          (recur))))

    ;; start order executor
    (subscribe-consumer executor-fn
                        event-topic
                        "UPDATE-ORDER")))

(defn start-application
  [order-executor-fn event-source-fn]
  (start-instrument-store)
  (start-order-store)
  (start-order-executor order-executor-fn)
  (start-event-source event-source-fn)
  :ok)
