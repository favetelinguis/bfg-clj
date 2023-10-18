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
  (let [{:keys [headers body]}(client! (rest/create-session-v2 config))
        cst (get headers "cst")
        token (get headers "x-security-token")
        ls-endpoint (:lightstreamerEndpoint body)]
    (merge config {:cst cst :token token :ls-endpoint ls-endpoint})))
