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
         [:p "What would you like to do?"]
         ]))

(defn market-main []
  (list (menu)
        [:main
         [:h1 "Handle markets"]
         [:form
          [:label {:for "search"} "Subscribe to epic:"]
          [:input#search {:name "epic" :type "search" :placeholder "Enter epic"}]
          [:button {:hx-post "/market/subscription" :hx-target "#market-list"} "Start subscription"]]
         [:div#market-list]]))

(defn account-list
  "TODO if its the active account
  if im subscribed or not
  the balance
  subscribe and unsubscribe button"
  [ms active-account-id subscribed-account-id]
  (for [{:keys [accountId accountName balance currency]} ms]
    (let [text (str accountName " has available " (:available balance) currency)]
      (cond
        (= accountId subscribed-account-id) [:li.text-blue-600 text]
        (= accountId active-account-id) [:li.text-red-600 text]
        (= accountId active-account-id subscribed-account-id) [:li.text-green-600 text]
        :else [:li text]))))

(defn account-main [ms active-account-id subscribed-account-id]
  (list (menu)
   [:main
    [:h1 "Handle account"]
    [:p "What would you like to do?"]
    (account-list ms active-account-id subscribed-account-id)]))

(defn portfolio-main []
  (list (menu)
        [:main
         [:h1 "Monitor portfolio"]
         [:p "What would you like to do?"]]))

(defn market-list [markets]
  (for [m markets]
    [:li m
     [:button {:hx-delete (str "/market/" m "/subscription")
               :hx-target "#market-list"
               :hx-swap "outerHTML"} "End subscription"]]))

(defn not-found []
  [:div
   [:h1 "404 Error!"]
   [:b "Page not found!"]
   [:p [:a {:href ".."} "Return to main page"]]])
