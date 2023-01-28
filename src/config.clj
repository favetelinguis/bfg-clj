(ns config
  (:require [clojure.edn :as edn]))

(defn get-env!
  [key]
  (if-let [val (System/getenv key)]
    val
    (throw
      (ex-info (str "Unable to read configuration for key " key) {:key key})))
  )

(defn load!
  []
  (->> (slurp ".config.edn")
       (edn/read-string {:readers {'ENV get-env!}})))
