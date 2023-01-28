(ns dev
  (:require
    [clojure.tools.namespace.repl :refer [refresh]]
    [bfg.account :as account]
    [java-http-clj.core :as http]
    [mount.core :refer [defstate start stop]]
    [clojure.core.async :as a]
    [config :as config]
    [bfg.system :as bfg]
    [ig.stream :as stream]
    [bfg-ig.setup :as bfg-ig]))

(defn start-listener
  [config bfg]
  (let [c (a/chan)
        callback (fn [event] (a/>! c event))]
    (stream/create-connection-and-subscriptions! config callback)
    (a/go-loop []
      (when-let [event (a/<! c)]
        ;; TODO here we should update bfg based on event
        (println event)
        (recur)))
    c))

(defn http-client
  [config bfg]
  (let [c (a/chan)]
    (a/go-loop []
      (when-let [request (a/<! c)]
        (http/send request)
        (recur)))
    c))

(defstate config
          :start (config/load!))

(defstate auth-context
          :start (atom (bfg-ig/create-session! config)))

(defstate bfg
          :start (atom (bfg/make-system (account/make (:account-id auth-context) nil nil))))


(defstate ls-listener
                :start (start-listener auth-context bfg)
                :stop (a/close! ls-listener))

(defn go []
  (let []
    (start)
    :ready))

(defn reset []
  (let []
    (stop)
    (refresh :after 'dev/go)))