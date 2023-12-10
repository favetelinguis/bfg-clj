(ns app.config
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]))

(defn load!
  []
  (let [config (edn/read-string (slurp ".config.edn"))
        secrets (edn/read-string (slurp ".secrets.edn"))]
    (merge config secrets)))

(defrecord Config [data]
  component/Lifecycle
  (start [this]
    (if data
      this
      (let [c (load!)]
        (assoc this :data c))))
  (stop [this] this))

(defn make
  []
  (map->Config {}))

(System/getenv)
