(ns ig.experiment
  (:require [clojure.edn :as edn]))

(defn get-env
  [key]
  key)

(->> (slurp ".config.edn")
    (edn/read-string {:readers {'ENV get-env}}))
