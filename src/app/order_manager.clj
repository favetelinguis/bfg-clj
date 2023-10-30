(ns app.order-manager
  (:require
   [clojure.core.async :as a]
   [ig.order-manager :as order-manager-thread]
   [ig.command-executor :refer [->IgCommandExecutor]]
   [com.stuartsierra.component :as component]))

(defrecord OrderManager [init rx order-state auth-context portfolio-fn]
  component/Lifecycle
  (start [this]
    (if init
      this
      (let [{:keys [http-client]} auth-context]
        (order-manager-thread/start rx portfolio-fn order-state (->IgCommandExecutor http-client))
        (assoc this :init true))))
  (stop [this]
    (when init
      (a/close! rx))
    (assoc this :rx false)))

(defn make [rx portfolio-in-chan]
  (map->OrderManager {:init false
                      :rx rx
                      :portfolio-fn #(a/>!! portfolio-in-chan %)
                      :order-state (atom {})})) ; TODO proper order cache state
