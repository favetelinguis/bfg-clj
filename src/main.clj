(ns main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]
            [clj-http.client :as client]
            [ig.setup :as ig-auth]
            [config]
            [app.web.server :as server-component]
            [app.market-generator :as market]
            [app.command-executor :as command-executor]
            [app.portfolio :as portfolio]
            [app.stream :as stream]
            )
  (:gen-class))


(defn create-system
  [{:keys [auth-context port]}]
  (component/system-map

   :stream
   (stream/new-stream auth-context)

   :command-executor
   (command-executor/new client/request)

   :portfolio
   (component/using (portfolio/new-portfolio)
                    [:command-executor])

   :market-generator
   (component/using (market/new-market-generator)
                    [:portfolio])

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
