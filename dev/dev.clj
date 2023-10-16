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
            [cheshire.core :as djson]))

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
  @(get-in system [:market-generator :state])
  (ig-authv/client! (igrest/create-session-v2 (config/load!)))
  ,)

#_(s/explain :bfg.market/event
          {:bfg.market/update-time "20:12:27", :bfg.market/market-delay nil, :bfg.market/market-state nil, :bfg.market/bid "15167.6", :bfg.market/offer "15170.4", :bfg.market/type :market/market-update, :bfg.market/epic "IX.D.DAX.IFMM.IP"})
