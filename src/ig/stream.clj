(ns ig.stream
  (:require
    [clojure.string :as str]
    [ig.client-listener-adapter :as client-listener-adapter])
  (:import (com.lightstreamer.client LightstreamerClient)))

(defn trade-pattern
  [account-id]
  {:pattern (str "TRADE:" account-id)
   :mode "MERGE"})

(defn account-pattern
  [account-id]
  {:pattern (str "ACCOUNT:" account-id)
   :mode "DISTINCT"})

(defn chart-candle-pattern
  "Possible scale SECOND, 1MINUTE, 5MINUTE, HOUR"
  [scale epic]
  {:pattern (str/join ":" ["CHART" epic scale])
   :mode "MERGE"})

(defn chart-candle-1min-pattern
  [epic]
  (partial chart-candle-pattern "1MINUTE"))

(defn create-connection-and-subscriptions!
  [auth-context callback]
  (let [{:keys [identifier cst token ls-endpoint]} (deref auth-context)
        password (str "CST-" cst "|XST-" token)
        connection-listener (client-listener-adapter/create callback)
        client (LightstreamerClient. ls-endpoint nil)]
    (doto (.-connectionDetails client)
      (.setPassword password)
      (.setUser identifier))
    (doto client
      (.addListener connection-listener)
      (.connect))))

(defn unsubscribe
  [client key])

(defn unsubscribe-all [lsclient subscriptions]
  (doseq [key subscriptions] (unsubscribe lsclient key)))