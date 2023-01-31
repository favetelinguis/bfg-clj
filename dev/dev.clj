(ns dev
  (:require
    [clojure.tools.namespace.repl :refer [refresh]]
    [mount.core :as mount])
  (:require                                                 ;; Mount states
    [app.auth-context]
    [app.config]
    [app.bfg]
    [app.stream]))

(defn go []
  (let []
    (mount/start)
    #_(mount/start #'app.config/basic-config
                 #'app.auth-context/auth-context
                 #'app.bfg/bfg
                 #'app.stream/stream)
    :ready))

(defn reset []
  (let []
    (mount/stop)
    (refresh :after 'dev/go)))
