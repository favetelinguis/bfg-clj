(ns main
  (:require [com.stuartsierra.component :as component]
            [app.order-manager :as order-manager]
            [app.config :as config]
            [app.auth-context :as auth-context]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component]
            [core.signal :as signal]
            [clojure.core.async :as a])
  (:gen-class))

(defn create-system
  [{:keys [port]}]
  (let [portfolio-in (a/chan)
        order-manager-in (a/chan)] ; TODO should this be buffered
    (component/system-map

     :config
     (config/make)

     :auth-context
     (component/using (auth-context/make)
                      [:config])

     :stream
     (component/using (stream-component/make)
                      [:config :auth-context])

     :order-manager
     (component/using (order-manager/make order-manager-in portfolio-in)
                      [:auth-context])

     :portfolio
     (portfolio-component/make portfolio-in
                               order-manager-in
                               [(signal/make-dax-killer-signal)
                                (signal/make-dax-killer-signal)
                                (signal/make-dax-killer-signal)])

     :web-server
     (component/using (server-component/make port)
                      [:auth-context :stream :portfolio :order-manager]))))

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

(component/start (create-system {:port 333}))
