(ns main
  (:require [com.stuartsierra.component :as component]
            [ig.setup :as ig-auth]
            [config]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component]
            )
  (:gen-class))


(defn create-system
  [{:keys [auth-context port]}]
  (component/system-map

   :stream
   (stream-component/new auth-context)

   :portfolio
   (portfolio-component/new auth-context)

   :web-server
   (component/using (server-component/new port)
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
