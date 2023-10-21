(ns main
  (:require [com.stuartsierra.component :as component]
            [ig.setup :as ig-auth]
            [config]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component]
            [org.httpkit.client :as client])
  (:gen-class))

(defn create-http-client [{:keys [base-url apikey cst token]}]
  (fn [{:keys [headers url]}]
    (client/request {:url (str base-url url)
                     :keep-alive 30000
                     :headers (merge {"Accept" "application/json; charset=UTF-8"
                                      "Content-Type" "application/json; charset=UTF-8"
                                      "X-IG-API-KEY" apikey
                                      "CST" cst
                                      "X-SECURITY-TOKEN" token} headers)
                     } (fn [{:keys [status headers body error opts]}]
                         (println "Request done with status " status "and body " body "or error " error)))))

(defn create-system
  [{:keys [auth-context port]}]
  (component/system-map

   :stream
   (stream-component/new auth-context)

   :portfolio
   (portfolio-component/new (create-http-client auth-context))

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
