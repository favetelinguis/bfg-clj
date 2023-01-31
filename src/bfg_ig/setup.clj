(ns bfg-ig.setup
  (:require
    [ig.rest :as rest]
    [clojure.data.json :as json]
    [java-http-clj.core :as http]))

(defn client!
  [request]
  (http/send request))

(defn create-session!
  "ig-config -> auth-context"
  [config]
  (let [response (client! (rest/create-session-v2 config))
        cst (get-in response [:headers "cst"])
        token (get-in response [:headers "x-security-token"])
        body (json/read-str (:body response))
        ls-endpoint (get body "lightstreamerEndpoint")
        auth-context (merge config {:cst cst :token token :ls-endpoint ls-endpoint})
        ]
    auth-context))
