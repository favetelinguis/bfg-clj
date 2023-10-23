(ns main
  (:require [com.stuartsierra.component :as component]
            [app.config :as config]
            [app.auth-context :as auth-context]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component])
  (:gen-class))

(defn create-system
  [{:keys [port]}]
  (component/system-map

   :config
   (config/new)

   :auth-context
   (component/using (auth-context/new)
                    [:config])

   :stream
   (component/using (stream-component/new)
                    [:config :auth-context])

   :portfolio
   (component/using (portfolio-component/new)
                    [:auth-context])

   :web-server
   (component/using (server-component/new port)
                    [:auth-context :stream :portfolio])))

(defn -main
  [& [port]]
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)
        system (-> {:port port}
                   (create-system)
                   (component/start)
                   (component/start-system))]
    (.addShutdownHook
     (Runtime/getRuntime)
     (new Thread #(component/stop-system system)))))
