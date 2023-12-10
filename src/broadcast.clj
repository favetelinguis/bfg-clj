(ns broadcast
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [clojure.test :as t]))
;;https://github.com/cgrand/xforms
;;https://astrecipes.net/blog/2016/11/24/transducers-how-to/
;;https://bsless.github.io/side-effects/
(defn ex-fn [error] (println "Error: " (.getMessage error)))

(defn make-state-transducer [f state]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result input]
       (let [[xs next] (f @state input)]
         (vreset! state next)
         (if (empty? xs)
           result
           (doseq [x xs]
             (xf result x))))))))

(defn make-strategy
  "f should be a function of form (fn [prev-state event] [[<sig events>] new-state])"
  [f start-state]
  (make-state-transducer f (volatile! start-state)))
(defn add-strategy [sstore x strategy mkets]
  (let [{:keys [mket-topic]} (:mstore sstore)
        {:keys [mix]} (:port sstore)
        {:keys [state]} sstore
        c (a/chan (a/sliding-buffer 1) strategy ex-fn)]
    (do
      (doseq [m mkets] ; subscribe to all makets
        (a/sub mket-topic m c))
      (a/admix mix c) ; add output to port
      (swap! state assoc x {:channel c
                            :mkets mkets}))))
(defn remove-strategy
  "remove strategy and remove from port input, do not unsubscribe from mstore but dont think its needed to unsubscribe"
  [sstore x]
  (let [{:keys [state]} sstore
        {:keys [mix]} (:port sstore)]
    (when-let [c (get-in @state [x :channel])]
      (a/unmix mix c)
      (a/close! c)
      (swap! state dissoc x))))

(defn o-store-state-transducer [state]
  (let [f (fn [old event]
            (println "o-store-x")
            (if (:create-o event) ; to break infinit lops only creae-o should be passed on to oexecutor
              [[{:new 3} {:new 3}] (merge old event)]
              [[] (merge old event)]))]
    (make-state-transducer f state)))

(defn m-store-state-transducer [state]
  (let [f (fn [old event]
            (println "m-store-x")
            [[{:cand 3} {:pri 3}] (merge old event)])]
    (make-state-transducer f state)))

(defn a-store-state-transducer [state]
  (let [f (fn [old event]
            (println "a-store-x")
            [[{:cand 3} {:pri 3}] (merge old event)])]
    (make-state-transducer f state)))

(defn port-state-transducer [state]
  (let [f (fn [old event]
            (println "port-x")
            [[{:new 3} {:new 3}] (merge old event)])]
    (make-state-transducer f state)))

(defrecord Connection [channel topic]
  component/Lifecycle
  (start [this]
    (println "Starting Connection")
    (if channel
      this
      (let [c (a/chan 1)]
        (-> this
            (assoc :channel c)
            (assoc :topic (a/pub c :route))))))
  (stop [this]
    (println "Stoping Connection")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defrecord MStore [state channel mket-topic connection]
  component/Lifecycle
  (start [this]
    (println "Starting MStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            state (volatile! {})
            c (a/chan 1 (m-store-state-transducer state) ex-fn)
            m-topic (a/pub c :mket)]
        (a/sub topic :mket c)
        (-> this
            (assoc :mket-topic m-topic)
            (assoc :state state)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping MStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defrecord OStore [state channel connection port]
  component/Lifecycle
  (start [this]
    (println "Starting OStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            port-c (:channel port)
            state (volatile! {})
            c (a/chan 1 (o-store-state-transducer state) ex-fn)
            mix (a/mix c)]
        (a/admix mix port-c)
        (a/sub topic :tade c)
        (-> this
            (assoc :state state)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping OStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defrecord AStore [state channel connection port]
  component/Lifecycle
  (start [this]
    (println "Starting AStore")
    (if channel
      this
      (let [{:keys [topic]} connection
            {:keys [mix]} port
            state (volatile! {})
            c (a/chan 1 (a-store-state-transducer state) ex-fn)]
        (a/sub topic :acc c)
        (a/admix mix c)
        (-> this
            (assoc :state state)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping AStore")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defrecord StrategyStore [state mstore port]
  component/Lifecycle
  (start [this]
    (println "Starting StrategyStore")
    (if state
      this
      (let [state (atom {})]
        (-> this
            (assoc :state state)))))
  (stop [this]
    (println "Stoping StrategyStore")
    (when state
      (assoc this :state nil))))

(defrecord OExecutor [channel ostore port]
  component/Lifecycle
  (start [this]
    (println "Starting OExecutor")
    (if channel
      this
      (let [o-c (:channel ostore)
            {:keys [mix]} port
            c (a/chan 1)]
        (a/admix mix c)
        (a/go-loop []
          (when-let [x (a/<! o-c)]
            (println "Inside OExecutor loop")
            (a/go (a/>! c {:acc x})))
          (recur))
        (-> this
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping OExecutor")
    (when channel
      (assoc this :channel nil))))

(defrecord Port [state channel mix]
  component/Lifecycle
  (start [this]
    (println "Starting Port")
    (if channel
      this
      (let [state (volatile! {})
            c (a/chan 1 (port-state-transducer state) ex-fn)
            port-mix (a/mix c)]
        (-> this
            (assoc :mix port-mix)
            (assoc :state state)
            (assoc :channel c)))))
  (stop [this]
    (println "Stoping Port")
    (when channel
      (a/close! channel)
      (assoc this :channel nil))))

(defn sys [config]
  (component/system-map
   :connection (map->Connection {})
   :port (component/using
          (map->Port {})
          [])
   :mstore (component/using
            (map->MStore {})
            [:connection])
   :astore (component/using
            (map->AStore {})
            [:connection :port])
   :ostore (component/using
            (map->OStore {})
            [:connection :port])
   ;; :i-store to handle all is
   ;; :strategy-store shold be here I guess to hold and manage all strategies not s1 s2 as individual strategies
   :strategy-store (component/using
                    (map->StrategyStore {})
                    [:mstore :port])
   :oexecutor (component/using
               (map->OExecutor {})
               [:ostore :port])))

(def system (sys {}))

(comment
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  @(get-in system [:strategy-store :state])
  (let [store (get-in system [:strategy-store])
        strategy-fn (fn [old event]
                      (println "strategy-x")
                      [[{:new 3} {:new 3}] (merge old event)])]
    (add-strategy store :mux100 (make-strategy strategy-fn {}) [:mux])
    (add-strategy store :fux10 (make-strategy strategy-fn {}) [:fux])
    (add-strategy store :rux1 (make-strategy strategy-fn {}) [:rux]))
  (remove-strategy (get-in system [:strategy-store]) :fux10)
  (let [c (get-in system [:connection :channel])]
    (a/>!! c {:route :tade})))

;; TESTS
(defn <!!?
  "Reads from chan synchronously, waiting for a given maximum of milliseconds.
  If the value does not come in during that period, returns :timed-out. If
  milliseconds is not given, a default of 1000 is used."
  ([chan]
   (<!!? chan 1000))
  ([chan milliseconds]
   (let [timeout (a/timeout milliseconds)
         [value port] (a/alts!! [chan timeout])]
     (if (= chan port)
       value
       :timed-out))))

(defn test-within
  "Asserts that ch does not close or produce a value within ms. Returns a
  channel from which the value can be taken."
  ([ch] (test-within 1000 ch))
  ([ch ms]
   (a/go (let [t (a/timeout ms)
               [v ch] (a/alts! [ch t])]
           (t/is (not= ch t)
                 (str "Test should have finished within " ms "ms."))
           v))))

;; (t/deftest test1
;;   (do
;;     (a/go (a/>! in-c {:route :mket}))
;;     (t/is (= {:cand 3} (<!!? (a/go (let [result-c (a/into [] m-store-c)]
;;                                      (a/close! m-store-c)
;;                                      (a/<! result-c))))))))

TODO Test mket flows throu the dynamically added strategies!
