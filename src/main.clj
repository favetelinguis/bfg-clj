(ns main
  (:require
    [mount.core :as mount])
  (:require
    [app.auth-context]
    [app.config]
    [app.bfg]
    [app.stream])
  (:gen-class))

(defn -main
  [& args]
  (println "ARGS" (str args))
  (mount/start)
  )