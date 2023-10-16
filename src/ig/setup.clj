(ns ig.setup
  (:require
    [ig.rest :as rest]
    [clj-http.client :as client]
    [cheshire.core :as json]))

(defn client!
  [m]
  (client/request m))

(defn create-session!
  "ig-config -> auth-context"
  [config]
  (let [response (client! (rest/create-session-v2 config))
        cst (get-in response [:headers "cst"])
        token (get-in response [:headers "x-security-token"])
        body (json/parse-string (:body response))
        ls-endpoint (get body "lightstreamerEndpoint")
        auth-context (merge config {:cst cst :token token :ls-endpoint ls-endpoint})
        ]
    auth-context))
