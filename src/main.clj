(ns main
  (:require
   [clojure.core.async :as a]
   [mount.core :as mount]
   [com.stuartsierra.component :as component])
  (:require
   [config]
   [bfg-ig.setup :as ig-auth]
   [app.market-generator :as market]
   [app.signal-generator :as signal]
   [app.portfolio :as portfolio]
   [app.stream :as stream])
  (:gen-class))

(defn create-system
  [auth-context]
  (component/system-map
   :market-generator (component/using (market/new-market-generator)
                                      [:signal-generator])
   :signal-generator (component/using (signal/new-signal-generator)
                                      [:portfolio])
   :portfolio (portfolio/new-portfolio)
   :stream (stream/new-stream auth-context)
   ;; :web-server (component/using () [:stream :market-generator :signal-generator :portfolio-service])
   ))

(defn -main
  [& args]
  (let [conf (config/load!)
        auth-context (ig-auth/create-session! conf)]
    (create-system auth-context)))

(comment
  (def config (config/load!))
  (def auth-context (bfg-ig.setup/create-session! config))
  (def system (create-system auth-context))
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  ,)
