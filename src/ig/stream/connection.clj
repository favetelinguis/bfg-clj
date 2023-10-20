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
  [{:keys [identifier cst token ls-endpoint]}]
  (let [password (str "CST-" cst "|XST-" token)
        connection-listener (client-listener)
        client (LightstreamerClient. ls-endpoint nil)]
    (doto (.-connectionDetails client)
      (.setPassword password)
      (.setUser identifier))
    (doto client
      (.addListener connection-listener)
      )))

(defn connect! [connection]
  (.connect connection))

(defn disconnect! [connection]
  ;; (unsubscribe-all! connection)
  (.disconnect connection))

(defn get-status [connection]
  (.getStatus connection))

(defn unsubscribe!
  [connection subscription]
  (.unsubscribe connection subscription))

(defn get-subscriptions [connection]
  (.getSubscriptions connection))

(defn subscribe! [connection subscription]
  (when (> max-subscriptions (count (get-subscriptions connection)))
    (.subscribe connection subscription)))

(defn unsubscribe-all! [connection]
  (str "TODO"))
