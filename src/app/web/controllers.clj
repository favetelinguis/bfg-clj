;; TODO use below as starts
;; (ns app.web.controllers
;;   (:require
;;    [ring.util.response :as resp]
;;    [bfg.portfolio :as portfolio]))

;; (defn create-market-subscription
;;   [req]
;;   (let [stream (-> req :application/stream :connection)
;;         epic (get-in req [:params :epic])]
;;     ;; Do the sideeffect
;;     (resp/redirect "/subscription/list")))

;; (defn query-portfolio
;;   [req]
;;   (let [*session (-> req :application/portfolio :session)
;;         result (portfolio/get-all-events @*session)]))

(ns app.web.controllers
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [GET POST PUT DELETE routes]]
            [ring.util.response :as response]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [compojure.route :as route]

            [app.web.views :as views]
            [bfg-ig.stream :as stream]))

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
     ;; "https://cdn.tailwindcss.com?plugins=forms"
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
               {:keys [connection]} (get-in request [:dependencies :stream])
               ; TODO subscription in channel to market-generator
               ; TODO we should also subscribe to candle data here.
               subscription (stream/new-market-subscription epic println)]
           (stream/subscribe! connection subscription)
           (response/redirect "/market/subscription/list" :see-other)))

    (GET "/market/subscription/list" request
         (let []
           ; TODO use stream to get all subscriptions and render them, also render a unsubscribe button, get input data for button not as form
           (-> (views/main)
               (ui-component)
               (ok))))

    (route/not-found
     (-> (views/not-found)
         (layout {:title (page-title "Error")})
         (ok))

     )))
