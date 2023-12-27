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
            [app.strategy-store :as strategy-store]
            [ig.stream.item :as i]
            [clojure.string :as str]))

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
           {:keys [rx rules-state]} (get-in request [:dependencies :portfolio])
           subscriptions (stream/get-subscriptions connection)
           candle-sub (subscription/new-candle-subscription (i/chart-candle-1min-item epic)
                                                            (fn [& more]
                                                              (doseq [m more]
                                                                (a/>!! rx m))) market-cache-state)
           market-sub (subscription/new-market-subscription epic (fn [& more]
                                                                   (doseq [m more]
                                                                     (a/>!! rx m))) market-cache-state)]
       (when (empty? (subscription/get-subscriptions-matching subscriptions (i/market-item epic)))
         ;; (swap! rules-state rules/subscribe-market epic)
         (stream/subscribe! connection market-sub candle-sub))
       (response/redirect "/market/subscription/list" :see-other)))

   (DELETE "/market/:epic/subscription" request
     (let [{:keys [epic]} (:params request)
           {:keys [connection market-cache-state]} (get-in request [:dependencies :stream])
           {:keys [rules-state]} (get-in request [:dependencies :portfolio])
           subs (stream/get-subscriptions connection)
           market-sub (first (subscription/get-subscriptions-matching subs (i/market-item epic)))
           candle-sub (first (subscription/get-subscriptions-matching subs (i/chart-candle-1min-item epic)))]
       (when market-sub
         (stream/unsubscribe! connection market-sub candle-sub)
         ;; (swap! rules-state rules/unsubscribe-market epic)
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
     (-> (views/account-main)
         (layout {:title (page-title "Account")})
         (ok)))

   (POST "/account/:account-id/subscription" request
     (let [{:keys [account-id]} (:params request)
           {:keys [connection]} (get-in request [:dependencies :stream])
           {:keys [rx]} (get-in request [:dependencies :portfolio])
           order-manager (get-in request [:dependencies :order-manager])
           subscriptions (stream/get-subscriptions connection)
           account-sub (subscription/new-account-subscription account-id #(a/>!! rx %))
           trade-sub (subscription/new-trade-subscription account-id #(a/>!! (:rx order-manager) %))]
       (when (empty? (subscription/get-subscriptions-matching subscriptions (i/account-item account-id)))
         (stream/subscribe! connection account-sub trade-sub))
       (response/redirect "/account/list" :see-other)))

   (DELETE "/account/:account-id/subscription" request
     (let [{:keys [account-id]} (:params request)
           {:keys [connection]} (get-in request [:dependencies :stream])
           subs (stream/get-subscriptions connection)
           account-sub (first (subscription/get-subscriptions-matching subs (i/account-item account-id)))
           trade-sub (first (subscription/get-subscriptions-matching subs (i/trade-item account-id)))]
       (do
         (stream/unsubscribe! connection account-sub trade-sub)
         (response/redirect "/account/list" :see-other))))

   (GET "/account/list" request
     (let [{:keys [http-client]} (get-in request [:dependencies :auth-context])
           {:keys [accounts]} @(http-client (rest/get-accounts))
           {:keys [accountId]} @(http-client (rest/get-session-details))
           {:keys [connection]} (get-in request [:dependencies :stream])
           sub (-> (stream/get-subscriptions connection)
                   (subscription/get-subscriptions-matching (i/account-item accountId))
                   (first)
                   (subscription/get-item)
                   (i/get-name))]
       (-> (views/account-list accounts accountId sub)
           ui-component
           ok)))

   (GET "/strategy" request
     (let [strategies (keys strategy-store/strategies)]
       (-> (views/strategy-main strategies)
           (layout {:title (page-title "Strategy")})
           (ok))))

   (GET "/strategy/list" request
     (let [{:keys [state]} (get-in request [:dependencies :strategy-store])]
       (-> (views/strategy-list (keys @state))
           (ui-component)
           (ok))))

   (POST "/strategy" request
     (let [{:keys [strategy epics]} (:params request)
           {:keys [state] :as store} (get-in request [:dependencies :strategy-store])]
       (strategy-store/add store strategy (clojure.string/split epics #","))
       (response/redirect "/strategy/list" :see-other)))

   (DELETE "/strategy/:key" request
     (let [{:keys [key]} (:params request)
           {:keys [state] :as store} (get-in request [:dependencies :strategy-store])]
       (strategy-store/delete store key)
       (response/redirect "/strategy/list" :see-other)))

   (GET "/signal/list" request
     (let [{:keys [rules-state]} (get-in request [:dependencies :portfolio])
           {:keys [connection]} (get-in request [:dependencies :stream])
           markets (-> (stream/get-subscriptions connection)
                       (subscription/get-subscribed-epics))
           strategies [] #_(rules/get-all-signals @rules-state)]
       (-> (views/signal-list strategies markets)
           (ui-component)
           (ok))))

   (PUT "/signal/:id" request
        ; TODO this is to basic a signal can be subscribed to multiple markets we support only one market now
     (let [{:keys [id epic]} (:params request)
           {:keys [rules-state]} (get-in request [:dependencies :portfolio])]
       (when (and epic id)
         ;; (swap! rules-state rules/activate-signal-for-market id epic)
         (response/redirect "/signal/list" :see-other))))

   (DELETE "/signal/:id" request
        ; TODO this is to basic a signal can be subscribed to multiple markets we support only one market now
     (let [{:keys [id epic]} (:params request)
           {:keys [rules-state]} (get-in request [:dependencies :portfolio])]
       (when (and epic id)
         ;; (swap! rules-state rules/deactivate-signal-for-market id epic)
         (response/redirect "/signal/list" :see-other))))

   (route/not-found
    (-> (views/not-found)
        (layout {:title (page-title "Error")})
        (ok)))))
