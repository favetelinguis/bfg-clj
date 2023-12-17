(ns ig.stream.item-test
  (:require [ig.stream.item :as sut]
            [clojure.test :as t]))

(t/deftest get-route-test
  (t/is (= "UNSUBSCRIBE" (sut/get-route "UNSUBSCRIBE:MARKET:id")))
  (t/is (= "UNSUBSCRIBE" (sut/get-route "UNSUBSCRIBE:CHART:id:1MINUTE")))
  (t/is (= "MARKET" (sut/get-route "MARKET:id")))
  (t/is (= "CHART" (sut/get-route "CHART:id:1MINUTE")))
  (t/is (= "TRADE" (sut/get-route "TRADE:id")))
  (t/is (= "ACCOUNT" (sut/get-route "ACCOUNT:id"))))

(t/deftest get-name-test
  (t/is (= "id" (sut/get-name "UNSUBSCRIBE:MARKET:id")))
  (t/is (= "id" (sut/get-name "UNSUBSCRIBE:CHART:id:1MINUTE")))
  (t/is (= "id" (sut/get-name "MARKET:id")))
  (t/is (= "id" (sut/get-name "CHART:id:1MINUTE")))
  (t/is (= "id" (sut/get-name "TRADE:id")))
  (t/is (= "id" (sut/get-name "ACCOUNT:id"))))
