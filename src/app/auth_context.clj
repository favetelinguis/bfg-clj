(ns app.auth-context
  (:require
    [bfg-ig.setup :as bfg-ig]
    [app.config :as conf]
    [mount.core :refer [defstate]]))

(defstate auth-context
          :start (atom (bfg-ig/create-session! conf/basic-config)))
