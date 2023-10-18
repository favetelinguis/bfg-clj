(ns ig.market-cache-test
  (:require [ig.market-cache :as sut]
            [clojure.test :as t]
            [core.events :as e]))


(t/deftest update-market-status
  (t/is (= [[] {"dax" {::sut/epic "dax"}}]
           (sut/update-status (sut/new) {::sut/epic "dax"})))

  (t/is (= [[] {"dax" {::sut/epic "dax"}
             "omx" {::sut/epic "omx"}}]
           (sut/update-status [[] {"dax" {::sut/epic "dax"}}]
                              {::sut/epic "omx"})))

  (t/is (= [[] {"dax" {::sut/epic "dax"}
             "omx" {::sut/epic "omx"
                    ::sut/state "OFFLINE"}}]
           (sut/update-status [[] {"dax" {::sut/epic "dax"}
                                "omx" {::sut/epic "omx"
                                       ::sut/state "CLOSED"}}]
                              {::sut/epic "omx"
                               ::sut/state "OFFLINE"
                               })))
  )

(t/deftest update-bid-offer-generate-events
  (t/is (= [[(e/create-bid-event "dax" "33")] {"dax" {::sut/epic "dax"
                                                      ::sut/bid "33"}}]
           (sut/update-status (sut/new) {::sut/epic "dax"
                                         ::sut/bid "33"})))
  )

(t/deftest delete-market
  (t/is (= {"dax" {::sut/epic "dax"}}
           (sut/remove-epic {"dax" {::sut/epic "dax"}
                               "omx" {::sut/epic "omx"}}
                              {::sut/epic "omx"})))
  )
