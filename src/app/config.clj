(ns app.config
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]))

(defn get-env!
  [key]
  (if-let [val (System/getenv key)]
    val
    (throw
      (ex-info (str "Unable to read configuration for key " key) {:key key}))))

(defn load!
  []
  (->> (slurp ".config.edn")
       (edn/read-string {:readers {'ENV get-env!}})))

(defrecord Config [data]
  component/Lifecycle
  (start [this]
         (if data
           this
           (let [c (load!)]
             (assoc this :data c))))
  (stop [this] this))

(defn new
  []
  (map->Config {}))
