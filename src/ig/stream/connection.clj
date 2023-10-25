; Only one connection is allowed, max 40 subscriptions is allowed on that connection.
(ns ig.stream.connection
  (:import (com.lightstreamer.client ClientListener LightstreamerClient)))

(def max-subscriptions 40)

(defn client-listener []
  (reify
    ClientListener
    (onListenEnd [this a] (println "onListenEnd" a))
    (onListenStart [this a] (println "onListenStart" a))
    (onServerError [this v1 v2] (println "onServerError" v1 v2))
    (onStatusChange [this status] (println "onStatusChange" status))))

(defn create-connection
  [{:keys [identifier]} {:keys [cst token lightstreamerEndpoint]}]
  (let [password (str "CST-" cst "|XST-" token)
        connection-listener (client-listener)
        client (LightstreamerClient. lightstreamerEndpoint nil)]
    (doto (.-connectionDetails client)
      (.setPassword password)
      (.setUser identifier))
    (doto client
      (.addListener connection-listener))))

(defn connect! [connection]
  (.connect connection))

(defn disconnect! [connection]
  (.disconnect connection))

(defn get-status [connection]
  (.getStatus connection))

(defn unsubscribe!
  [connection & subscriptions]
  (doseq [subscription subscriptions]
    (.unsubscribe connection subscription)))

(defn get-subscriptions [connection]
  (.getSubscriptions connection))

(defn subscribe! [connection & subscriptions]
  (when (> max-subscriptions (+ (count subscriptions) (count (get-subscriptions connection))))
    (doseq [subscription subscriptions]
      (.subscribe connection subscription))))

(defn unsubscribe-all! [connection]
  (doseq [s (get-subscriptions connection)]
    (unsubscribe! connection s)))
