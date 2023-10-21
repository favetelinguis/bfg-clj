(ns ig.setup
  (:require
    [ig.rest :as rest]
    [org.httpkit.client :as client]
    [cheshire.core :as json]))

(defn client!
  [m]
  (client/request m))

(defn create-session!
  "ig-config -> auth-context"
  ;TODO need error handling
  [config]
  (let [{:keys [headers body]} @(client! (rest/create-session-v2 config))
        {:keys [cst x-security-token]} headers
        {:keys [lightstreamerEndpoint]} (json/decode body true)]
    (merge config {:cst cst :token x-security-token :ls-endpoint lightstreamerEndpoint})))
