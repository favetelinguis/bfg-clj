(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [hiccup2.core :as hv]
            [config]
            [ig.setup :as ig-authv]
            [main]
            [app.web.views :as devv]
            [ig.stream.connection :as igstream]
            [ig.stream.subscription :as igsubscription]
            [ig.rest :as igrest]
            [clojure.spec.alpha :as ds]
            [cheshire.core :as djson]
            [clj-http.client :as client]))

(def conf (config/load!))

#_(def auth-context (ig-authv/create-session! conf))

#_(set-init
  (fn [_]
    (let []
      (main/create-system
       {:port 3000
        :auth-context auth-context}))))

(comment
  (hv/html (devv/market-main))
  (reset)
  (start)
  (stop)
  (ns-unalias 'dev 'igstream)
  (:market-generator system)
  (let [connection (:connection (:stream system))]
    (igsubscription/get-market-data-subscription
     (igstream/get-subscriptions connection)
     "IX.D.DAX.IFMM.IP"))
  @(get-in system [:market-generator :state])
  (ig-authv/client! (igrest/open-order (config/load!)))
  (ig-authv/create-session! conf)
  ,)

(client/request {:url     "https://google.com"
                 :async? true
                 :respond (fn [response] (println "response is:" response))
                 ; raise will be called for all non 2xx and 3xx responses
                 :raise (fn [exception] (println "exception message is: " (.getMessage exception)))})
