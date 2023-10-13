(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [hiccup2.core :as h]
            [config]
            [bfg-ig.setup :as ig-auth]
            [main]
            [app.web.views :as views]
            [bfg-ig.stream :as stream]
            [clojure.spec.alpha :as s]))

(set-init
  (fn [_]
    (let [conf (config/load!)
          auth-context (ig-auth/create-session! conf)
          ]
      (main/create-system
       {:port 3000
        :auth-context auth-context}))))

(comment
  (h/html (views/market-main))
  (reset)
  (ns-unalias 'dev 'stream)

  (let [connection (:connection (:stream system))]
    (.isActive(first (stream/get-subscriptions connection))))
  ,)

#_(s/explain :bfg.market/event
          {:bfg.market/update-time "20:12:27", :bfg.market/market-delay nil, :bfg.market/market-state nil, :bfg.market/bid "15167.6", :bfg.market/offer "15170.4", :bfg.market/type :market/market-update, :bfg.market/epic "IX.D.DAX.IFMM.IP"})
