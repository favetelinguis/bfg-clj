(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [hiccup2.core :as hv]
            [main]
            [app.web.views :as devv]
            [ig.stream.connection :as igstream]
            [ig.stream.subscription :as igsubscription]
            [ig.rest :as igrest]
            [clojure.spec.alpha :as ds]
            [cheshire.core :as djson]
            [org.httpkit.client :as client]
            [ig.rest :as rest]))

(set-init
  (fn [_]
    (main/create-system {:port 3000})))

(comment
  (hv/html (devv/market-main))
  (reset)
  (start)
  (stop)
  (ns-unalias 'dev 'igstream)
  (:market-generator system)
  (let [connection (:connection (:stream system))]
    ;; (igstream/get-subscriptions connection)
    (igstream/get-status connection)
    )
  @(get-in system [:stream :market-cache-state])
  @((get-in system [:stream :http-client]) (rest/set-active-account "Z53ZLX"))
  (Double/parseDouble "2.3332")
;; https://rymndhng.github.io/2020/04/15/production-considerations-for-clj-http/
@(client/request {:url     "https://google.com"
                 :keep-alive 30000

                 } (fn [{:keys [status headers body error opts]}]
                     body))
  ,)


