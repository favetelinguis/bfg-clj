(ns app.web.views)

(defn main []
  [:div
   [:h1 "Hello Hiccup Page with Routing!"]
   [:p "What would you like to do?"]
   [:p [:a {:href "./market"} "Handle markets"]]
   [:p [:a {:href "./portfolio"} "View portfolio"]]
   [:p [:a {:href "./account"} "Handle account"]]])

(defn market-main []
  [:div
   [:h1 "Handle markets"]
   [:form
    [:label {:for "search"} "Subscribe to epic:"]
    [:input {:id "search" :name "epic" :type "search" :placeholder "Enter epic"}]
    [:button {:hx-post "/market/subscription" :hx-target "#market-list"} "Start subscription"]]
   [:div {:id "market-list"}]])

(defn account-main []
  [:div
   [:h1 "Handle account"]
   [:p "What would you like to do?"]
   [:p [:a {:href "./market"} "Handle markets"]]
   [:p [:a {:href "./portfolio"} "View portfolio"]]
   [:p [:a {:href "./account"} "Handle account"]]])

(defn portfolio-main []
  [:div
   [:h1 "Monitor portfolio"]
   [:p "What would you like to do?"]
   [:p [:a {:href "./market"} "Handle markets"]]
   [:p [:a {:href "./portfolio"} "View portfolio"]]
   [:p [:a {:href "./account"} "Handle account"]]])

(defn not-found []
  [:div
   [:h1 "404 Error!"]
   [:b "Page not found!"]
   [:p [:a {:href ".."} "Return to main page"]]])