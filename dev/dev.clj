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
            [org.httpkit.client :as client]))

(def conf (config/load!))

(def auth-context (ig-authv/create-session! conf))

(set-init
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
  (ig-authv/client! (igrest/open-order (config/load!)))
  (ig-authv/create-session! conf)
  @(get-in system [:stream :market-cache-state])
  (Double/parseDouble "2.3332")
;; https://rymndhng.github.io/2020/04/15/production-considerations-for-clj-http/
@(client/request {:url     "https://google.com"
                 :keep-alive 30000

                 } (fn [{:keys [status headers body error opts]}]
                     body))
  ,)


