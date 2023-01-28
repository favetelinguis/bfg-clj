(ns main
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [java-http-clj.core :as http])
    (:gen-class))

(defn get-env
  [key]
  (if-let [val (System/getenv key)]
    val
    (throw
      (ex-info (str "Unable to read configuration for key " key) {:key key})))
  )

(defn get-config
  []
  (->> (slurp ".config.edn")
          (edn/read-string {:readers {'ENV get-env}})))

(def config (get-config))

(defn create-session-v2
  [{:keys [base-url identifier password apikey]}]
  {:headers {"accept"       "application/json"
             "content-type" "application/json"
             "version"      "2"
             "x-ig-api-key" apikey}
   :method  :post
   :uri     (str base-url "/session")
   :body    (json/write-str {"identifier" identifier "password" password})}
  )

(defn execute-http-request
  [request]
  (http/send request))

(defn create-session
  "ig-config -> auth-context"
  [{:keys [apikey] :as ig-config}]
  (let [response (execute-http-request (create-session-v2 ig-config))
        cst (get-in response [:headers "cst"])
        token (get-in response [:headers "x-security-token"])
        body (json/read-str (:body response))
        ls-endpoint (get body "lightstreamerEndpoint")
        ] {:apikey apikey :cst cst :token token :ls-endpoint ls-endpoint}))

(create-session (:ig config))

(s/def ::auth-context (s/keys :req-un [::apikey ::cst ::sct]))

(defn -main
      [& args]
      (println "Hello" (str args)))