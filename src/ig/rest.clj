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
  [{:keys [baseUrl identifier password apikey]}]
  {:headers {"version"      "2"
             "Accept" "application/json; charset=UTF-8"
             "Content-Type" "application/json; charset=UTF-8"
             "x-ig-api-key" apikey}
   :url     (str baseUrl "/session")
   :method :post
   :body    (json/encode {:identifier identifier :password password})})

(defn open-order [m]
  {:headers {"version"      "1"}
   :method :get
   :url     "/accounts/preferences"})

(defn close-order [m]
  {:headers {"version"      "1"}
   :method :get
   :url     "/accounts/preferences"})

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
