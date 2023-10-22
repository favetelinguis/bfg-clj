(ns main
  (:require [com.stuartsierra.component :as component]
            [ig.setup :as ig-auth]
            [config]
            [app.web.server :as server-component]
            [app.portfolio :as portfolio-component]
            [app.stream :as stream-component]
            [org.httpkit.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(defn create-http-client [{:keys [baseUrl apikey cst token]}]
  ; TODO make this into a component and move login/logout to start stop
  ; TODO Make so one can send in custom callback but have one as default value how to :or work for default value?
  (fn [{:keys [headers url method body]}]
    (client/request {:url (str baseUrl url)
                     :keep-alive 30000
                     :method method
                     :body body
                     :headers (merge {"Accept" "application/json; charset=UTF-8"
                                      "Content-Type" "application/json; charset=UTF-8"
                                      "X-IG-API-KEY" apikey
                                      "CST" cst
                                      "X-SECURITY-TOKEN" token} headers)
                     } (fn [{:keys [status headers body error opts]}]
                         (if (= status 200)
                           (json/decode body true)
                           (do
                             (println "Failure with status " status " error " error " and body " body)))))))

(defn create-system
  [{:keys [auth-context port]}]
  (let [http-client (create-http-client auth-context)]
    (component/system-map

     :stream
     (stream-component/new auth-context http-client)

     :portfolio
     (portfolio-component/new http-client)

     :web-server
     (component/using (server-component/new port http-client)
                      [:stream :portfolio]))))

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
