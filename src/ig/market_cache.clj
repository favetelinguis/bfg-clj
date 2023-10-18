(ns ig.market-cache
  (:require [clojure.spec.alpha :as s]
            [core.events :as e]))

;; {::events []
;;  "epic1" {::status
;;           ::candle...}}

(s/def ::update-time (s/nilable string?))
(s/def ::epic (s/nilable string?))
(s/def ::delay (s/nilable string?))
(s/def ::state (s/nilable #{"CLOSED" "OFFLINE" "TRADEABLE" "EDIT" "AUCTION" "AUCTION_NO_EDIT" "SUSPENDED"}))

(s/def ::bid (s/nilable string?)) ;; remove
(s/def ::offer (s/nilable string?)) ;; remove

(defn new []
  [[] {}])

(defn update-status
  [[_ old-m] {:keys [::epic ::bid ::offer] :as new-m}]
  [(remove nil? [(when bid
                   (e/create-bid-event epic bid))
                 (when offer
                   (e/create-offer-event epic offer))])
   (update old-m epic merge new-m)])

(defn remove-epic
  [old-m {:keys [::epic]}]
  (dissoc old-m epic))
