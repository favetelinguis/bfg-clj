(ns main
  (:require [com.stuartsierra.component :as component]
            [app.config :as config]
            [app.auth-context :as auth-context]
            [app.application :as app-component]
            [app.strategy-store :as strategy-store-component]
            [app.web.server :as server-component]
            [app.stream :as stream-component])
  (:gen-class))

(defn create-system
  [{:keys [port]}]
  (component/system-map

   :config
   (config/make)

   :auth-context
   (component/using (auth-context/make)
                    [:config])

   :application
   (component/using (app-component/make)
                    [:auth-context])

   :stream
   (component/using (stream-component/make)
                    [:config :auth-context :application])

   :strategy-store
   (component/using (strategy-store-component/make)
                    [:application :stream])

   :web-server
   (component/using (server-component/make port)
                    [:auth-context :strategy-store])))
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
