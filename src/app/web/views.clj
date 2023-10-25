(ns app.web.views)

(defn menu []
  [:nav
   [:a {:href "./market"} "Handle markets"]
   [:a {:href "./portfolio"} "View portfolio"]
   [:a {:href "./account"} "Handle account"]])

(defn main []
  (list (menu)
        [:main
         [:h1 "Hello Hiccup Page with Routing!"]
         [:p "What would you like to do?"]]))

(defn market-main []
  (list (menu)
        [:main
         [:h1 "Handle markets"]
         [:form
          [:label {:for "search"} "Subscribe to epic:"]
          [:input#search {:name "epic" :type "search" :placeholder "Enter epic"}]
          [:button {:hx-post "/market/subscription"
                    :hx-target "#subscription-market-list"} "Start subscription"]]
         [:div#subscription-market-list {:hx-get "/market/subscription/list"
                                         :hx-trigger "load"}]]))

(defn account-list
  "We only show and support the account that is defult and active for current session"
  [ms active-account-id subscription-account-id]
  (for [{:keys [accountId accountName balance currency]} ms]
    (when (= active-account-id accountId)
      (let [text (str accountId accountName " has available " (:available balance) currency)
            subscribe-button [:button {:hx-post (str "/account/" accountId "/subscription")
                                       :hx-target "#account-list"} "Subscribe"]
            unsubscribe-button [:button {:hx-delete (str "/account/" accountId "/subscription")
                                         :hx-target "#account-list"} "End subscription"]]
        (cond
          (= accountId active-account-id subscription-account-id) [:li.text-green-600 text unsubscribe-button]
          :else [:li text subscribe-button])))))

(defn account-main []
  (list (menu)
        [:main
         [:h1 "Handle account"]
         [:p "What would you like to do?"]
         [:div#account-list {:hx-get "/account/list"
                             :hx-trigger "load"}]]))

(defn portfolio-main []
  (list (menu)
        [:main
         [:h1 "Monitor portfolio"]
         [:p "What would you like to do?"]]))

(defn market-list [markets]
  (for [m markets]
    [:li m
     [:button {:hx-delete (str "/market/" m "/subscription")
               :hx-target "#subscription-market-list"} "End subscription"]]))

(defn not-found []
  [:div
   [:h1 "404 Error!"]
   [:b "Page not found!"]
   [:p [:a {:href ".."} "Return to main page"]]])
