(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [hiccup2.core :as hv]
            [main]
            [app.web.views :as devv]
            [clojure.spec.test.alpha :as stest]
            [ig.stream.connection :as igstream]
            [ig.stream.subscription :as igsubscription]
            [ig.rest :as igrest]
            [clojure.spec.alpha :as ds]
            [cheshire.core :as djson]
            [org.httpkit.client :as client]
            [ig.rest :as rest]
            [ig.stream.subscription :as subscription]
            [ig.stream.connection :as stream]
            [core.events :as e]
            [clojure.core.async :as a]))

(set-init
 (fn [_]
   (main/create-system {:port 3000})))

(comment
  (hv/html (devv/market-main))
  (reset)
  (start)
  (stop)
  (get-in system [:auth-context :http-client])
  (ns-unalias 'dev 'igstream)
  (:market-generator system)

  (def mysub (let [connection (:connection (:stream system))
                   market-sub (subscription/new-market-subscription "IX.D.DAX.IFMM.IP"
                                                                    (fn [event]
                                                                      (println event)))]
               (stream/subscribe! connection market-sub)
               market-sub))

  (let [connection (:connection (:stream system))]
    ;; (stream/get-subscriptions connection)
    (stream/unsubscribe! connection mysub))
  (stest/instrument `unsubscribe)
  (clojure.core.async/>!! (get @(get-in system [:strategy-store :state]) "DAXKiller_IX.D.DAX.IFMM.IP") (e/balance {::e/name "hej" ::e/balance 3333}))
  (clojure.core.async/<!! (get @(get-in system [:strategy-store :state]) "DAXKiller_IX.D.DAX.IFMM.IP"))
  @((get-in system [:auth-context :http-client]) (rest/open-order  "IX.D.DAX.IFMM.IP" "BUY" 1 "EUR"))
  @((get-in system [:auth-context :http-client]) (rest/close-order "DIAAAANL43VHHA8" "SELL" 1))
  (Double/parseDouble "2.3332")
;; https://rymndhng.github.io/2020/04/15/production-considerations-for-clj-http/
  @(client/request {:url     "https://google.com"
                    :keep-alive 30000} (fn [{:keys [status headers body error opts]}]
                                         body)))
