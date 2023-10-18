(ns ig.rest
  (:require
   [cheshire.core :as json]))

(defn create-session-v2
  ;; {:clientId "103607452",
  ;; :trailingStopsEnabled false,
  ;; :currentAccountId "Z53ZLW",
  ;; :currencySymbol "SEK",
  ;; :dealingEnabled true,
  ;; :hasActiveLiveAccounts true,
  ;; :accountInfo
  ;; {:balance 99947.75, :deposit 0.0, :profitLoss 0.0, :available 99947.75},
  ;; :hasActiveDemoAccounts true,
  ;; :lightstreamerEndpoint "https://demo-apd.marketdatasystems.com",
  ;; :reroutingEnvironment nil,
  ;; :timezoneOffset 2,
  ;; :accountType "CFD",
  ;; :currencyIsoCode "SEK",
  ;; :accounts
  ;; [{:accountId "Z53ZLW",
  ;;   :accountName "CFD",
  ;;   :preferred true,
  ;;   :accountType "CFD"}
  ;;  {:accountId "Z53ZLX",
  ;;   :accountName "Barriers och optioner",
  ;;   :preferred false,
  ;;   :accountType "CFD"}
  ;;  {:accountId "Z53ZLY",
  ;;   :accountName "Börshandlade produkter",
  ;;   :preferred false,
  ;;   :accountType "PHYSICAL"}]}
  [{:keys [base-url identifier password apikey]}]
  {:headers {"version"      "2"
             "x-ig-api-key" apikey}
   :accept :json
   :content-type :json
   :method  :post
   :socket-timeout 5000
   :conn-timeout   5000
   :as  :json ; Response is in json
   :url     (str base-url "/session")
   :body    (json/generate-string {:identifier identifier :password password})}
  )

(defn open-order
  [{:keys [base-url identifier password apikey]}]
  {:headers {"version"      "2"
             "x-ig-api-key" apikey}
   :accept :json
   :content-type :json
   :method  :post
   :socket-timeout 5000
   :conn-timeout   5000
   :as  :json ; Response is in json
   :url     (str base-url "/session")
   :body    (json/generate-string {:identifier identifier :password password})
   :async? true
   :respond (fn [response] (println "response is:" response))
  ; raise will be called for all non 2xx and 3xx responses
   :raise (fn [exception] (println "exception message is: " (.getMessage exception)))})
