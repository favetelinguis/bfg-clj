(ns ig.client-listener-adapter
  (:import (com.lightstreamer.client ClientListener)))

(defn create [callback]
  (reify
    ClientListener
    (onListenEnd [this a] (println "onListenEnd"))
    (onListenStart [this a] (println "onListenStart"))
    (onServerError [this v1 v2] (println v1 " " v2))
    (onStatusChange [this status] (println status))
    ))