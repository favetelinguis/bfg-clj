(ns ig.order-manager.order-cache
  (:require [core.events :as e]))

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
;; {:dealIdOrigin DIAAAANL45UBMAL, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 1, :level 14707.9, :dealReference PPUVZFA4JACTYPT, :status OPEN, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T17:01:34.703, :dealId DIAAAANL45UBMAL, :direction SELL}
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T17:01:34.713, :expiry -, :affectedDeals [{:dealId DIAAAANL45UBMAL, :status OPENED}], :profit nil, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14707.9, :dealReference PPUVZFA4JACTYPT, :reason SUCCESS, :status OPEN, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency nil, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL45UBMAL, :direction SELL}
;; Close
;; {:dealIdOrigin DIAAAANL45UBMAL, :stopLevel nil, :dealStatus ACCEPTED, :expiry -, :channel PublicRestOTC, :size 0, :level 14710.7, :dealReference PPUVZFA4JACTYPT, :status DELETED, :epic IX.D.DAX.IFMM.IP, :limitLevel nil, :ig.order-manager/kind :opu, :guaranteedStop false, :timestamp 2023-10-30T17:02:01.144, :dealId DIAAAANL45UBMAL, :direction SELL}
;; {:stopLevel nil, :dealStatus ACCEPTED, :date 2023-10-30T17:02:01.152, :expiry -, :affectedDeals [{:dealId DIAAAANL45UBMAL, :status FULLY_CLOSED}], :profit -33.29, :channel PublicRestOTC, :limitDistance nil, :size 1, :level 14710.7, :dealReference UAU9JJHPNWYTYPT, :reason SUCCESS, :status CLOSED, :epic IX.D.DAX.IFMM.IP, :trailingStop false, :profitCurrency SEK, :limitLevel nil, :ig.order-manager/kind :confirms, :stopDistance nil, :guaranteedStop false, :dealId DIAAAANL45UBMAL, :direction BUY}
(defn make [] {})

(defn has-order?
  [cache epic]
  (not (nil? (get cache epic))))

(defn add-order
  [cache {:keys [::e/name] :as event}]
  (assoc cache name event))

(defn remove-order
  [cache epic]
  (dissoc cache epic))
