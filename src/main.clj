(ns main
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [clojure.core.async :as a]
   [compojure.coercions :refer [as-int]]
   [compojure.core :refer [GET POST PUT DELETE let-routes]]
   [compojure.route :as route]
   [com.stuartsierra.component :as component])
  (:require
   [config]
   [app.web.controllers :as controller]
   [app.web.server :as server-component]
   [bfg-ig.setup :as ig-auth]
   [app.market-generator :as market]
   [app.signal-generator :as signal]
   [app.portfolio :as portfolio]
   [app.stream :as stream])
  (:gen-class))


(defn create-system
  [{:keys [auth-context port]}]
  (component/system-map
   :market-generator
   (component/using (market/new-market-generator)
                    [:signal-generator])

   :signal-generator
   (component/using (signal/new-signal-generator)
                    [:portfolio])

   :portfolio
   (portfolio/new-portfolio)

   :stream
   (stream/new-stream auth-context)

   :web-server
   (component/using (server-component/new-http-server-component port)
                    [:stream :portfolio])))

(defn -main
  [& [port]]
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)
        conf (config/load!)
        auth-context (ig-auth/create-session! conf)
        system (-> {:port port
                    :auth-context auth-context}
                   (create-system)
                   (component/start)
                   (component/start-system))]
    (.addShutdownHook
     (Runtime/getRuntime)
     (new Thread #(component/stop-system system)))))
