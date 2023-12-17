(ns ig.account-cache-test
  (:require [ig.account-cache :as sut]
            [clojure.test :as t]))

(t/deftest account-update
  (t/is (= {} (sut/update-cache {} {"ROUTE" "ACCOUNT:id"}))))
