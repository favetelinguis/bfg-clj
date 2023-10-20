(ns ig.market-cache-test
  (:require [ig.market-cache :as sut]
            [clojure.test :as t]
            [core.events :as e]))


(t/deftest update-market-status
  (t/is (= [[] {"dax" {"NAME" "dax"}}]
           (sut/update-status (sut/new) {"NAME" "dax"})))

  ; nil keys in update is not supported
  (t/is (= [[] {"dax" {"NAME" "dax" "MARKET_STATE" nil}}]
           (sut/update-status [[] {"dax" {"NAME" "dax" "MARKET_STATE" "CLOSED"}}]
                              {"NAME" "dax" "MARKET_STATE" nil})))

  (t/is (= [[] {"dax" {"NAME" "dax"}
                "omx" {"NAME" "omx"}}]
           (sut/update-status [[] {"dax" {"NAME" "dax"}}]
                              {"NAME" "omx"})))

  (t/is (= [[] {"dax" {"NAME" "dax"}
                "omx" {"NAME" "omx"
                       "MARKET_STATE" "OFFLINE"}}]
           (sut/update-status [[] {"dax" {"NAME" "dax"}
                                "omx" {"NAME" "omx"
                                       "MARKET_STATE" "CLOSED"}}]
                              {"NAME" "omx"
                               "MARKET_STATE" "OFFLINE"
                               })))
  )

(t/deftest update-bid-offer-generate-events
  (t/is (= [[(e/create-bid-event "dax" "33")] {"dax" {"NAME" "dax"
                                                      "BID" "33"}}]
           (sut/update-status (sut/new) {"NAME" "dax"
                                         "BID" "33"})))
  )

(t/deftest delete-market
  (t/is (= [[] {"dax" {"NAME" "dax"}}]
           (sut/remove-epic [[] {"dax" {"NAME" "dax"} "omx" {"NAME" "omx"}}] "omx")))
  )
