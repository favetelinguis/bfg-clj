(ns user
  (:require
    [clojure.core.async :as a]
    [java-http-clj.core :as http]))


(defn http-client
  [config bfg]
  (let [c (a/chan)]
    (a/go-loop []
      (when-let [request (a/<! c)]
        (http/send request)
        (recur)))
    c))


