(ns core.event.market
  (:require [clojure.spec.alpha :as s]))

(s/def ::update-time (s/nilable string?))
(s/def ::market-delay (s/nilable string?))
(s/def ::market-state (s/nilable #{"CLOSED" "OFFLINE" "TRADEABLE" "EDIT" "AUCTION" "AUCTION_NO_EDIT" "SUSPENDED"}))
(s/def ::bid (s/nilable string?))
(s/def ::offer (s/nilable string?))
(s/def ::account string?)
