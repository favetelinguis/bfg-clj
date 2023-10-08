(ns dev
  (:require [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [config]
            [bfg-ig.setup :as ig-auth]
            [main]))

(set-init
  (fn [_]
    (let [conf (config/load!)
          auth-context (ig-auth/create-session! conf)
          ]
      (main/create-system
       {:port 3000
        :auth-context auth-context}))))

(comment
  (reset)
  ,)
