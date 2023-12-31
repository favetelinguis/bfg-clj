(ns ig.order-cache-test
  (:require [ig.order-cache :as sut]
            [clojure.test :as t]
            [ig.cache :as cache]
            [core.events :as e]
            [clojure.spec.test.alpha :as stest]
            [clojure.set :as set]))

(stest/instrument)

;; Accept 3x trade events
;; CONFIRMS OPU WOU
;; Once a CONFIRMS comes in move from order-initiated -> order-created
;; Send order_created event to portfolio
;; order-created -> order-closed send to portfolio
;; order-created -> position-created
;; position-created -> position-closed send to portfolio

;; Accept ORDER-CREATE events
;; If order-create event check if there is any order for market if so ignore order
;; Add state order-initiated on key market, only one order/market is allowed then
;; Make async call to IG to create order if call fails remove order initiated, order client is rate limited

; Open new market order
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T16:39:08.138, :expiry -, :affectedDeals [{:dealId DIAAAANL43VHHA8, :status OPENED}], :profit nil, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14714.2, :dealReference BXURXNUWV6CTYPT, :reason SUCCESS, :status OPEN, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency nil, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL43VHHA8, :direction BUY}
;; {:dealIdOrigin DIAAAANL43VHHA8, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 1, :level 14714.2, :dealReference BXURXNUWV6CTYPT, :status OPEN, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T16:39:08.131, :dealId DIAAAANL43VHHA8, :direction BUY}
;;
;; Failure closing order
;; {:stopLevel nil, :dealStatus REJECTED, :date 2023-10-30T16:49:00.626, :expiry nil, :affectedDeals [], :profit nil, :channel PublicRestOTC, :limitDistance nil, :size nil, :level nil, :dealReference 3ACHAUR7JCGTYPT, :reason POSITION_ALREADY_EXISTS_IN_OPPOSITE_DIRECTION, :status nil, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency nil, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL44Z8KAP, :direction BUY}

;; Close that order using delete endpoint - observe direction must be oposite what I hade for open
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T16:53:56.4, :expiry -, :affectedDeals [{:dealId DIAAAANL43VHHA8, :status FULLY_CLOSED}], :profit -74.88, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14707.9, :dealReference 8EAUXK4DS28TYPT, :reason SUCCESS, :status CLOSED, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency SEK, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL43VHHA8, :direction SELL}
;; {:dealIdOrigin DIAAAANL43VHHA8, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 0, :level 14707.9, :dealReference BXURXNUWV6CTYPT, :status DELETED, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T16:53:56.393, :dealId DIAAAANL43VHHA8, :direction BUY}
;;
;;-----------------------------
;;
;; Open and close using open order
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T17:01:34.713, :expiry -, :affectedDeals [{:dealId DIAAAANL45UBMAL, :status OPENED}], :profit nil, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14707.9, :dealReference PPUVZFA4JACTYPT, :reason SUCCESS, :status OPEN, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency nil, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL45UBMAL, :direction SELL}
;; {:dealIdOrigin DIAAAANL45UBMAL, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 1, :level 14707.9, :dealReference PPUVZFA4JACTYPT, :status OPEN, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T17:01:34.703, :dealId DIAAAANL45UBMAL, :direction SELL}
;; Close
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T17:02:01.152, :expiry -, :affectedDeals [{:dealId DIAAAANL45UBMAL, :status FULLY_CLOSED}], :profit -33.29, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14710.7, :dealReference UAU9JJHPNWYTYPT, :reason SUCCESS, :status CLOSED, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency SEK, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL45UBMAL, :direction BUY}
;; {:dealIdOrigin DIAAAANL45UBMAL, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 0, :level 14710.7, :dealReference PPUVZFA4JACTYPT, :status DELETED, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T17:02:01.144, :dealId DIAAAANL45UBMAL, :direction SELL}
;;
(def rejected-confirms-order
  {:stopLevel nil
   :dealStatus "REJECTED"
   :date "2023-10-30T16:49:00.626"
   :expiry nil
   :affectedDeals []
   :profit nil
   :channel "PublicRestOTC"
   :limitDistance nil
   :size nil
   :level nil
   :dealReference "3ACHAUR7JCGTYPT"
   :reason "POSITION_ALREADY_EXISTS_IN_OPPOSITE_DIRECTION"
   :status nil
   :epic "IX.D.DAX.IFMM.IP"
   :trailingStop false
   :profitCurrency nil
   :limitLevel nil
   :stopDistance nil
   :guaranteedStop false
   :dealId "DIAAAANL44Z8KAP"
   :direction "BUY"})

(def accepted-confirms-order
  {:stopLevel nil
   :dealStatus "ACCEPTED"
   :date "2023-10-30T16:39:08.138"
   :expiry "-"
   :affectedDeals [{:dealId "DIAAAANL43VHHA8" :status "OPENED"}]
   :profit nil
   :channel "PublicRestOTC"
   :limitDistance nil
   :size 1
   :level 14714.2
   :dealReference "BXURXNUWV6CTYPT"
   :reason "SUCCESS"
   :status "OPEN"
   :epic "IX.D.DAX.IFMM.IP"
   :trailingStop false
   :profitCurrency nil
   :limitLevel nil
   :stopDistance nil
   :guaranteedStop false
   :dealId "DIAAAANL43VHHA8"
   :direction "BUY"})

(def filled-order
  {:dealIdOrigin "DIAAAANL43VHHA8"
   :stopLevel nil
   :dealStatus "ACCEPTED"
   :expiry -
   :channel "PublicRestOTC"
   :size 0
   :level 14707.9
   :dealReference "BXURXNUWV6CTYPT"
   :status "DELETED"
   :epic "IX.D.DAX.IFMM.IP"
   :limitLevel nil
   :guaranteedStop false
   :timestamp "2023-10-30T16:53:56.393"
   :dealId "DIAAAANL43VHHA8"
   :direction "BUY"})

;; (t/deftest create-first-order-propegate-order-event
;;   (let [e (e/create-new-order "dax" "SELL" 3)]
;;     (t/is (= (cache/make [e] {"dax" :order-initiated})
;;              (sut/update-cache (cache/make) e)))))

;; (t/deftest create-order-when-already-open-does-nothing
;;   (let [initial {"dax" :order-initiated}]
;;     (t/is (= (cache/make initial)
;;              (sut/update-cache (cache/make initial)
;;                                (e/create-new-order "dax" "BUY" 333))))))

;; (t/deftest delete-order-non-exist
;;   (let [e (e/exit "dax")]
;;     (t/is (= (cache/make)
;;              (sut/update-cache (cache/make) e)))))

;; (t/deftest delete-order-when-exist
;;   (let [initial {"dax" :order-initiated}
;;         e (e/exit "dax")]
;;     (t/is (= (cache/make [e] {})
;;              (sut/update-cache (cache/make initial)
;;                                e)))))

;; (t/deftest failure-open-order-remove-existing-and-signal-failure
;;   (let [initial {"IX.D.DAX.IFMM.IP" :order-initiated}]
;;     (t/is (= (cache/make [(e/exit "IX.D.DAX.IFMM.IP")] {})
;;              (sut/update-confirms (cache/make initial)
;;                                   rejected-confirms-order)))))

;; (t/deftest failure-open-order-not-existing
;;   (let [initial {"OTHER_EPIC" :order-initiated}]
;;     (t/is (= (cache/make initial)
;;              (sut/update-confirms (cache/make initial)
;;                                   rejected-confirms-order)))))

;; (t/deftest success-open-order-update-status-cache
;;   (let [initial {"IX.D.DAX.IFMM.IP" :order-initiated}
;;         after-update {"IX.D.DAX.IFMM.IP" :order-confirmed}]
;;     (t/is (= (cache/make [] after-update)
;;              (sut/update-confirms (cache/make initial)
;;                                   accepted-confirms-order)))))

;; (t/deftest success-open-not-existing-order-do-nothing
;;   (let [initial {"OTHER_EPIC" :order-initiated}]
;;     (t/is (= (cache/make [] initial)
;;              (sut/update-confirms (cache/make initial)
;;                                   accepted-confirms-order)))))

(t/deftest ACCOUNT-event
  (let [start (cache/make {"SOME" {:d 1}})]
    (t/is (= (cache/combine start (cache/make {::e/balance 99701.17}))
             (sut/update-cache start
                               (e/signal-update "ACCOUNT" {::e/name "Z53ZLW"
                                                           "AVAILABLE_CASH" "99701.17"})))
          "add balance to cache")
    (t/is (= start
             (sut/update-cache start
                               (e/signal-update "ACCOUNT" {::e/name "Z53ZLW"
                                                           "AVAILABLE_CASH" "AA99701.17"})))
          "non numeric balance")

    (t/is (= start
             (sut/update-cache start
                               (e/signal-update "ACCOUNT" {::e/name "Z53ZLW"
                                                           "WRONG" "99701.17"})))
          "incorrect key")))

(t/deftest SIGNAl-event
  (let [start {"dax" (sut/make-entry :order_close_initiated
                                     (e/signal {::e/name "dax" ::e/direction "BUY"}))}
        event (e/signal {::e/name "dax" ::e/direction "SELL"})
        [events new] (sut/update-cache (cache/make start) event)]
    (t/is (= 0 (count events)))
    (t/is (= 1 (count (keys new)))))
  (let [start {"dax" (sut/make-entry :order_initiated
                                     (e/signal {::e/name "dax" ::e/direction "BUY"}))}
        event (e/signal {::e/name "dax" ::e/direction "SELL"})
        [events new] (sut/update-cache (cache/make start) event)]
    (t/is (= 1 (count events)))
    (t/is (= 1 (count (keys new)))))
  (let [start {"dax" (sut/make-entry :order_initiated
                                     (e/signal {::e/name "dax" ::e/direction "BUY"}))}
        event (e/signal {::e/name "dax" ::e/direction "BUY"})
        [events new] (sut/update-cache (cache/make start) event)]
    (t/is (= 0 (count events)))
    (t/is (= 1 (count (keys new)))))
  (let [start {"omx" (sut/make-entry :order_initiated
                                     (e/signal {::e/name "omx" ::e/direction "BUY"}))}
        event (e/signal {::e/name "dax" ::e/direction "BUY"})
        [events new] (sut/update-cache (cache/make start) event)]
    (t/is (= 1 (count events)))
    (t/is (= 2 (count (keys new))))))

(t/deftest UPDATE-ORDER--event
  (let [start {"dax" (sut/make-entry :order_close_initiated
                                     (e/signal {::e/name "dax" ::e/direction "BUY"}))}
        event (e/order-failure {::e/name "dax"})
        [events new] (sut/update-cache (cache/make start) event)]
    (t/is (= 1 (count events)))
    (t/is (sut/order-failure? new "dax"))))

;; TODO
;; No Update-order event if market is closed
;; #:core.events{:id #uuid "0535653e-b409-4646-9c9c-d93962fe36f0", :action "MARKET", :data {"MARKET_DELAY" "0", "MARKET_STATE" "EDIT", :core.events/name "IX.D.DAX.IFMM.IP"}, :timestamp #object[java.time.Instant 0x
;;
;; Do the change-order-state fns work?
