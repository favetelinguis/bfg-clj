(ns app.auth-context
  (:require [com.stuartsierra.component :as component]
            [ig.rest :as rest]
            [org.httpkit.client :as client]
            [cheshire.core :as json]))

(defn create-session!
  "ig-config -> auth-context"
  ;TODO need error handling
  [config]
  (let [{:keys [headers body]} @(client/request (rest/create-session-v2 config))
        {:keys [cst x-security-token]} headers]
    (merge (json/decode body true) {:cst cst :token x-security-token})))

(defn create-http-client [{:keys [baseUrl apikey]} {:keys [cst token]}]
  "Returns a function that takes a request map and the following option parameters
  :error-callback function with signatur [status raw-body]
  :success-callback function with signatur [body-as-keyword-map]"
  (fn [{:keys [headers url method body]} & {:keys [error-callback success-callback] :or {error-callback (fn [status body] (println "Failure with status " status " and body " body))
                                                                                         success-callback identity}}]
    (client/request {:url (str baseUrl url)
                     :keep-alive 30000
                     :method method
                     :body body
                     :headers (merge {"Accept" "application/json; charset=UTF-8"
                                      "Content-Type" "application/json; charset=UTF-8"
                                      "X-IG-API-KEY" apikey
                                      "CST" cst
                                      "X-SECURITY-TOKEN" token} headers)} (fn [{:keys [status headers body error opts]}]
                                                                            (if (< status 299)
                                                                              (success-callback (json/decode body true))
                                                                              (error-callback status body))))))

(defrecord AuthContext [http-client session config]
  component/Lifecycle
  (start [this]
    (if http-client
      this
      (let [session-details (create-session! (:data config))
            c (create-http-client (:data config) session-details)]
        (-> this
            (assoc :session session-details)
            (assoc :http-client c)))))
  (stop [this]
    (if http-client
      (do
        (http-client (rest/logout))
        (assoc this :http-client nil))
      this)))

(defn make
  []
  (map->AuthContext {}))
