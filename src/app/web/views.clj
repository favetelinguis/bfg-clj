(ns app.web.views)

(defn main []
  [:div
   [:h1 "Hello Hiccup Page with Routing!"]
   [:p "What would you like to do?"]
   [:p [:a {:href "./get-form.html"} "Submit a GET request"]]
   [:p [:a {:href "./post-form.html"} "Submit a POST request"]]])

(defn not-found []
  [:div
   [:h1 "404 Error!"]
   [:b "Page not found!"]
   [:p [:a {:href ".."} "Return to main page"]]])
