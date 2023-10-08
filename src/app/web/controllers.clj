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
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [compojure.route :as route]

            [app.web.views :as views]))

(defn- page-title
  [title]
  (str title " | Example Application"))

(defn- layout
  [body {:keys [title]}]
  [:head
   [:meta {:charset "UTF-8"}]
   [:title title]
   (hp/include-js
     "https://cdn.tailwindcss.com?plugins=forms"
     "https://unpkg.com/htmx.org@1.9.4")
   [:body {:hx-boost "true"} body]])

(defn- ok
  ([body headers]
   {:status 200
    :headers (merge {"Content-Type" "text/html"}
                    headers)
    :body (-> body
              (h/html)
              (str))})
  ([body]
   (ok body {})))

(def app-routes
  (routes
    (GET "/" request
         (-> (views/main)
             (layout {:title (page-title "Home")})
             (ok)))

    (PUT "/stream/market/:epic/subscribe" request
         (let [{:keys [epic]} (:params request)]
           (-> (views/main)
               (layout {:title (page-title (str "EPIC: " epic))})
               (ok))))

    (route/not-found
     (-> (views/not-found)
         (layout {:title (page-title "NOT FOUND")})
         (ok))

     )))
