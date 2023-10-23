(ns app.web.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.logger :as logger]
            [app.web.controllers :as controllers]))

(defn wrap-dependencies
  [handler dependencies]
  (fn [request]
    (handler (assoc request :dependencies dependencies))))

(defrecord HttpServerComponent
    [port steam portfolio auth-context web-server]
  component/Lifecycle
  (start [this]
    (println "Starting HttpServerCompoent")
    (if web-server
      this
      (let [http-server (jetty/run-jetty
                         (-> controllers/app-routes
                             (keyword-params/wrap-keyword-params)
                             (params/wrap-params)
                             (logger/wrap-with-logger)
                             (wrap-dependencies this))
                         {:port port
                          :join? false})]
        (assoc this :web-server http-server))))
  (stop [this]
    (println "Stopping HttpServerComponent")
    (when-let [server (:web-server this)]
      (.stop server))
    (assoc this :web-server nil)))

(defn new
  [port]
  (map->HttpServerComponent {:port port}))
