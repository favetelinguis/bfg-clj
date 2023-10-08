(ns app.web.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST PUT DELETE routes]]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [hiccup.page :as hp]
            [hiccup2.core :as h]

            [app.web.views :as views]
            [app.web.controllers :as controllers]))

(defn wrap-dependencies
  [handler dependencies]
  (fn [request]
    (handler (assoc request :dependencies dependencies))))

(defrecord HttpServerComponent
    [port steam portfolio web-server]
  component/Lifecycle
  (start [this]
    (println "Starting HttpServerCompoent")
    (if web-server
      this
      (let [http-server (jetty/run-jetty
                         (-> controllers/app-routes
                             (keyword-params/wrap-keyword-params)
                             (params/wrap-params)
                             (wrap-dependencies this))
                         {:port port
                          :join? false})]
        (assoc this :web-server http-server))))
  (stop [this]
    (println "Stopping HttpServerComponent")
    (when-let [server (:web-server this)]
      (.stop server))
    (assoc this :web-server nil)))

(defn new-http-server-component
  [port]
  (map->HttpServerComponent {:port port}))
