(ns app.bfg
  (:require
    [mount.core :refer [defstate]]
    [app.auth-context :as context]
    [bfg.account :as account]
    [bfg.system :as bfg]))

(defstate bfg
          :start (atom (bfg/make-system (account/make (:account-id context/auth-context) nil nil))))
