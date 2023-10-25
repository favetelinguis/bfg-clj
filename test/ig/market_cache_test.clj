(ns ig.market-cache-test
  (:require [ig.market-cache :as sut]
            [clojure.test :as t]
            [core.events :as e]))

(t/deftest update-market-status
  (t/is (= (sut/make {"dax" {"NAME" "dax"}})
           (sut/update-status (sut/make) {"NAME" "dax"})))

  ; nil keys in update is not supported
  (t/is (= (sut/make {"dax" {"NAME" "dax" "MARKET_STATE" nil}})
           (sut/update-status (sut/make {"dax" {"NAME" "dax" "MARKET_STATE" "CLOSED"}})
                              {"NAME" "dax" "MARKET_STATE" nil})))

  (t/is (= (sut/make {"dax" {"NAME" "dax"}
                      "omx" {"NAME" "omx"}})
           (sut/update-status (sut/make {"dax" {"NAME" "dax"}})
                              {"NAME" "omx"})))

  (t/is (= (sut/make {"dax" {"NAME" "dax"}
                      "omx" {"NAME" "omx"
                             "MARKET_STATE" "OFFLINE"}})
           (sut/update-status (sut/make {"dax" {"NAME" "dax"}
                                         "omx" {"NAME" "omx"
                                                "MARKET_STATE" "CLOSED"}})
                              {"NAME" "omx"
                               "MARKET_STATE" "OFFLINE"}))))

(def start-candle-update {"OFR_OPEN" "14826.1"
                          "NAME" "IX.D.DAX.IFMM.IP"
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
                        "NAME" "IX.D.DAX.IFMM.IP"})

(def end-candle-update {"OFR_CLOSE" "14827.6"
                        "BID_CLOSE" "14824.8"
                        "CONS_END" "1"
                        "NAME" "IX.D.DAX.IFMM.IP"})

(t/deftest update-candle
  (t/is (= [(list (e/create-mid-price-event "IX.D.DAX.IFMM.IP" 14824.2))
            {"IX.D.DAX.IFMM.IP"
             {"OFR_OPEN" "14826.1",
              "NAME" "IX.D.DAX.IFMM.IP",
              "BID_OPEN" "14823.3",
              "BID_CLOSE" "14822.8",
              "BID_LOW" "14821.3",
              "OFR_LOW" "14824.1",
              "CONS_END" "0",
              "BID_HIGH" "14823.3",
              "OFR_CLOSE" "14825.6",
              "OFR_HIGH" "14826.1",
              "UTM" "1698255900000"}}]
           (-> (sut/make)
               (sut/update-candle start-candle-update))))
  (t/is (= ['({::e/name "IX.D.DAX.IFMM.IP",
               ::e/price 14826.2,
               ::e/kind :core.events/mid-price}
              {::e/name "IX.D.DAX.IFMM.IP",
               ::e/open 14824.7,
               ::e/high 14824.7,
               ::e/low 14822.7,
               ::e/close 14826.2,
               ::e/time "1698255900000",
               ::e/kind ::e/candle})
            {"IX.D.DAX.IFMM.IP"
             {"OFR_OPEN" "14826.1",
              "NAME" "IX.D.DAX.IFMM.IP",
              "BID_OPEN" "14823.3",
              "BID_CLOSE" "14824.8",
              "BID_LOW" "14821.3",
              "OFR_LOW" "14824.1",
              "CONS_END" "1",
              "BID_HIGH" "14823.3",
              "OFR_CLOSE" "14827.6",
              "OFR_HIGH" "14826.1",
              "UTM" "1698255900000"}}]
           (-> (sut/make)
               (sut/update-candle start-candle-update)
               (sut/update-candle mid-candle-update)
               (sut/update-candle end-candle-update)))))

(t/deftest delete-market
  (t/is (= [[] {"dax" {"NAME" "dax"}}]
           (sut/remove-epic [[] {"dax" {"NAME" "dax"} "omx" {"NAME" "omx"}}] "omx"))))
