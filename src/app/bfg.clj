(ns app.bfg
  (:require
    [mount.core :refer [defstate]]
    [bfg-ig.rules :as rules]
    [odoyle.rules :as o]))

(defstate bfg
          :start (atom (reduce o/add-rule (o/->session) rules/rule-set)))
