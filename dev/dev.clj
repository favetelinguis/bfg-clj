(ns dev
  (:require
    [clojure.tools.namespace.repl :refer [refresh]]
    [mount.core :as mount])
  )

(defn go []
  (let []
    (mount/start #'app.config/basic-config
                 #'app.auth-context/auth-context
                 #'app.bfg/bfg
                 #'app.stream/stream)
    :ready))

(defn reset []
  (let []
    (mount/stop)
    (refresh :after 'dev/go)))
