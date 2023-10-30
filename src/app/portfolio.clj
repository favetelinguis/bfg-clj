(ns app.portfolio
  (:require
   [clojure.core.async :as a]
   [com.stuartsierra.component :as component]
   [core.portfolio :as portfolio-thread]
   [core.portfolio.rules :as rules]))

(defrecord Portfolio [init rx rules-state]
  component/Lifecycle
  (start [this]
    (if init
      this
      (do
        (portfolio-thread/start rx rules-state)
        (assoc this :init true))))
  (stop [this]
    (when init
      (a/close! rx))
    (assoc this :init false)))

(defn make [rx order-manager-chan signals]
  (map->Portfolio {:init false
                   :rx rx
                   :rules-state (atom (rules/create-session #(a/>!! order-manager-chan %) signals))}))
