(ns ig.rest
  (:require
   [clj-http.client :as client]
   [cheshire.core :as json]))

(defn do-request
  [m]
  (client/request m))

(defn create-session-v2
  [{:keys [base-url identifier password apikey]}]
  {:headers {"accept"       "application/json"
             "content-type" "application/json"
             "version"      "2"
             "x-ig-api-key" apikey}
   :method  :post
   :url     (str base-url "/session")
   :body    (json/generate-string {"identifier" identifier "password" password})}
  )
