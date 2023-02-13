(ns bfg-ig.rest
  (:require [clojure.data.json :as json]))

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
