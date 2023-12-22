(ns ig.rest
  (:require
   [cheshire.core :as json]))

(defn create-session-v2
  [{:keys [baseUrl identifier password apikey]}]
  {:headers {"version"      "2"
             "Accept" "application/json; charset=UTF-8"
             "Content-Type" "application/json; charset=UTF-8"
             "x-ig-api-key" apikey}
   :url     (str baseUrl "/session")
   :method :post
   :body    (json/encode {:identifier identifier :password password})})

(defn get-accounts []
  {:headers {"version"      "1"}
   :method :get
   :url     "/accounts"})

(defn get-session-details []
  {:headers {"version"      "1"}
   :method :get
   :url     "/session"})

(defn logout []
  {:headers {"version"      "1"}
   :method :delete
   :url     "/session"})

(defn open-order [epic direction size currencyCode]
  {:headers {"version"      "2"}
   :method :post
   :url     "/positions/otc"
   :body (json/encode {"epic" epic
                       "direction" direction
                       "size" size
                       "expiry" "-" ; could vary between markets?
                       "dealReference" epic
                       "currencyCode" currencyCode
                       "forceOpen" false
                       "guaranteedStop" false
                       "orderType" "MARKET"})})

(defn close-order [deal-id direction size]
  {:headers {"version"      "1"
             "_method" "DELETE"}
   :method :get
   :url          "/positions/otc"
   :body (json/encode {"dealId" deal-id
                       "direction" direction
                       "size" size
                       "orderType" "MARKET"})})
