(ns main
  (:require [com.stuartsierra.component :as component]
            [app.config :as config]
            [app.auth-context :as auth-context]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component]
            [core.signal :as signal])
  (:gen-class))

(defn create-system
  [{:keys [port]}]
  (component/system-map

   :config
   (config/make)

   :auth-context
   (component/using (auth-context/make)
                    [:config])

   :stream
   (component/using (stream-component/make)
                    [:config :auth-context])

   :portfolio
   (component/using (portfolio-component/make [(signal/make-dax-killer-signal)
                                               (signal/make-dax-killer-signal)
                                               (signal/make-dax-killer-signal)])
                    [:auth-context])

   :web-server
   (component/using (server-component/make port)
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
