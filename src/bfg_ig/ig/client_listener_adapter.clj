(ns ig.client-listener-adapter
  (:import (com.lightstreamer.client ClientListener)))

(defn create [callback]
  (reify
    ClientListener
    (onListenEnd [this a] (callback {:onListenEnd a}))
    (onListenStart [this a] (callback {:onListenStart a}))
    (onServerError [this v1 v2] (callback {:onServerError (str v1 v2)}))
    (onStatusChange [this status] (callback {:onStatusChange status}))
    ))