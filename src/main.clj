(ns main
  (:require [com.stuartsierra.component :as component]
            [app.config :as config]
            [app.auth-context :as auth-context]
            [app.instrument-store :as instrument-store-component]
            [app.account-store :as account-store-component]
            [app.order-store :as order-store-component]
            [app.strategy-store :as strategy-store-component]
            [app.order-executor :as order-executor-component]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component])
  (:gen-class))

; TODO what thread is used to run all strategy?
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
   (portfolio-component/make)

   :instrument-store
   (component/using (instrument-store-component/make)
                    [:stream])

   :account-store
   (component/using (account-store-component/make)
                    [:stream :portfolio])

   :order-store
   (component/using (order-store-component/make)
                    [:stream :portfolio])

   :strategy-store
   (component/using (strategy-store-component/make)
                    [:instrument-store :portfolio :stream])

   :order-executor
   (component/using (order-executor-component/make)
                    [:order-store :portfolio :auth-context])

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
