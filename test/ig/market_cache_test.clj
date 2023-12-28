(ns ig.market-cache-test
  (:require [ig.market-cache :as sut]
            [clojure.test :as t]
            [core.events :as e]
            [ig.cache :as cache])
  (:import [java.time Instant]))

(t/deftest update-market-status
  (t/is (= (cache/make {"dax" {"ROUTE" "MARKET:dax"}})
           (sut/update-status (cache/make) {"ROUTE" "MARKET:dax"})))

  ; nil keys in update is not supported
  (t/is (= (cache/make {"dax" {"ROUTE" "MARKET:dax" "MARKET_STATE" nil}})
           (sut/update-status (cache/make {"dax" {"ROUTE" "MARKET:dax" "MARKET_STATE" "CLOSED"}})
                              {"ROUTE" "MARKET:dax" "MARKET_STATE" nil})))

  (t/is (= (cache/make {"dax" {"ROUTE" "MARKET:dax"}
                        "omx" {"ROUTE" "MARKET:omx"}})
           (sut/update-status (cache/make {"dax" {"ROUTE" "MARKET:dax"}})
                              {"ROUTE" "MARKET:omx"})))

  (t/is (= (cache/make {"dax" {"ROUTE" "MARKET:dax"}
                        "omx" {"ROUTE" "MARKET:omx"
                               "MARKET_STATE" "OFFLINE"}})
           (sut/update-status (cache/make {"dax" {"ROUTE" "MARKET:dax"}
                                           "omx" {"ROUTE" "MARKET:omx"
                                                  "MARKET_STATE" "CLOSED"}})
                              {"ROUTE" "MARKET:omx"
                               "MARKET_STATE" "OFFLINE"}))))

(def start-candle-update {"OFR_OPEN" "14826.1"
                          "ROUTE" "CHART:IX.D.DAX.IFMM.IP"
                          "BID_OPEN" "14823.3"
                          "BID_CLOSE" "14822.8"
                          "BID_LOW" "14821.3"
                          "OFR_LOW" "14824.1"
                          "CONS_END" "0"
                          "BID_HIGH" "14823.3"
                          "OFR_CLOSE" "14825.6"
                          "OFR_HIGH" "14826.1"
                          "UTM" "1698255900000"})

(def mid-candle-update {"OFR_CLOSE" "14827.1"
                        "BID_CLOSE" "14824.3"
                        "ROUTE" "CHART:IX.D.DAX.IFMM.IP"})

(def end-candle-update {"OFR_CLOSE" "14827.6"
                        "BID_CLOSE" "14824.8"
                        "CONS_END" "1"
                        "ROUTE" "CHART:IX.D.DAX.IFMM.IP"})

(t/deftest update-candle
  (t/is (= [(list (e/mid-price {::e/name "IX.D.DAX.IFMM.IP" ::e/price 14824.2}))
            {"IX.D.DAX.IFMM.IP"
             {"OFR_OPEN" "14826.1",
              "ROUTE" "CHART:IX.D.DAX.IFMM.IP",
              "BID_OPEN" "14823.3",
              "BID_CLOSE" "14822.8",
              "BID_LOW" "14821.3",
              "OFR_LOW" "14824.1",
              "CONS_END" "0",
              "BID_HIGH" "14823.3",
              "OFR_CLOSE" "14825.6",
              "OFR_HIGH" "14826.1",
              "UTM" "1698255900000"}}]
           (-> (cache/make)
               (sut/update-candle start-candle-update))))
  (t/is (= (cache/make [{::e/name "IX.D.DAX.IFMM.IP",
                         ::e/price 14826.2,
                         ::e/kind :core.events/mid-price}
                        {::e/name "IX.D.DAX.IFMM.IP",
                         ::e/open 14824.7,
                         ::e/high 14824.7,
                         ::e/low 14822.7,
                         ::e/close 14826.2,
                         ::e/time (Instant/parse "2023-10-25T17:45:00Z"),
                         ::e/kind ::e/candle}]
                       {"IX.D.DAX.IFMM.IP"
                        {"OFR_OPEN" "14826.1",
                         "ROUTE" "CHART:IX.D.DAX.IFMM.IP",
                         "BID_OPEN" "14823.3",
                         "BID_CLOSE" "14824.8",
                         "BID_LOW" "14821.3",
                         "OFR_LOW" "14824.1",
                         "CONS_END" "1",
                         "BID_HIGH" "14823.3",
                         "OFR_CLOSE" "14827.6",
                         "OFR_HIGH" "14826.1",
                         "UTM" "1698255900000"}})
           (-> (cache/make)
               (sut/update-candle start-candle-update)
               (sut/update-candle mid-candle-update)
               (sut/update-candle end-candle-update)))))

(t/deftest delete-market
  (t/is (= [[] {"dax" {"ROUTE" "UNSUBSCRIBE:MARKET:dax"}}]
           (sut/remove-epic (cache/make {"dax" {"ROUTE" "UNSUBSCRIBE:MARKET:dax"}
                                         "omx" {"ROUTE" "UNSUBSCRIBE:MARKET:omx"}})
                            {"ROUTE" "UNSUBSCRIBE:MARKET:omx"}))))
