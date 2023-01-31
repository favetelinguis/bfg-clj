(ns app.config
  (:require
    [config :as c]
    [mount.core :refer [defstate]]))

(defstate basic-config
          :start (c/load!))

