(ns app.web.controllers
  (:require [compojure.core :refer [GET POST PUT DELETE routes]]
            [ring.util.response :as response]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [compojure.route :as route]
            [app.web.views :as views]
            [ig.stream.subscription :as subscription]
            [ig.stream.connection :as stream]
            [clojure.core.async :as a]
            [ig.market-cache :as market-cache]
            [ig.rest :as rest]
            [ig.stream.item :as i]))

(defn- page-title
  [title]
  (str title " | Example Application"))

(defn- layout
  "HTMX is smart enogh when using hx-boost that if I return html head tags it will be removed, only title will be changed so all
  top level pages that I want to swap in with body can use this layout function."
  [body {:keys [title]}]
  (hp/html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:title title]
    (hp/include-js
     "https://cdn.tailwindcss.com?plugins=forms"
     "https://unpkg.com/htmx.org@1.9.4")
    [:body {:hx-boost "true"} body]]))

(defn- ui-component
  [body]
  (-> body
      (h/html)
      (str)))

(defn- ok
  "body string representation of html"
  ([body headers]
   {:status 200
    :headers (merge {"Content-Type" "text/html"}
                    headers)
    :body body})
  ([body]
   (ok body {})))

(def app-routes
  (routes
    (GET "/" request
         (-> (views/main)
             (layout {:title (page-title "Home")})
             (ok)))

    (GET "/market" request
         (-> (views/market-main)
             (layout {:title (page-title "Markets")})
             (ok)))

    (POST "/market/subscription" request
         (let [{:keys [epic]} (:params request)
               {:keys [connection market-cache-state]} (get-in request [:dependencies :stream])
               {:keys [rx]} (get-in request [:dependencies :portfolio])
               ; TODO we should also subscribe to candle data here.
               ; TODO we should not be able to subscribe to something we already have subscribed to
               sub (subscription/new-market-subscription epic (fn [& more]
                                                                (doseq [m more]
                                                                  (a/>!! rx m))) market-cache-state)]
           (stream/subscribe! connection sub)
           (response/redirect "/market/subscription/list" :see-other)))

    (DELETE "/market/:epic/subscription" request
         (let [{:keys [epic]} (:params request)
               {:keys [connection market-cache-state]} (get-in request [:dependencies :stream])
               subs (stream/get-subscriptions connection)
               sub (subscription/get-market-data-subscription subs epic)]
           (when sub
             (stream/unsubscribe! connection sub)
             (swap! market-cache-state market-cache/remove-epic epic))
           (response/redirect "/market/subscription/list" :see-other)))

    (GET "/market/subscription/list" request
         (let [{:keys [connection]} (get-in request [:dependencies :stream])]
           (-> connection
               stream/get-subscriptions
               subscription/get-subscribed-epics
               views/market-list
               ui-component
               ok)))

    (GET "/account" request
         (let [{:keys [http-client]} (:dependencies request)
               {:keys [accounts]} @(http-client (rest/get-accounts))
               {:keys [accountId]} @(http-client (rest/get-session-details))
               {:keys [connection]} (get-in request [:dependencies :stream])
               subs (stream/get-subscriptions connection)
               sub (i/get-name
                    (subscription/get-item
                     (subscription/get-account-subscription subs)))]
           (-> (views/account-main accounts accountId sub)
               (layout {:title (page-title "Account")})
               (ok))))

    (route/not-found
     (-> (views/not-found)
         (layout {:title (page-title "Error")})
         (ok)))))
