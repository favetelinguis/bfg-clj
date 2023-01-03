(ns bfg.state
  (:require
    [clojure.spec.gen.alpha :as gen]
    [clojure.spec.alpha :as s]))

(s/def ::ostate keyword?)
(s/def ::error string?)
(s/def ::value double?)

(defmulti ostate-type ::ostate)
(defmethod ostate-type :ostate/init [_]
  (s/keys :req [::value]))
(defmethod ostate-type :ostate/error [_]
  (s/keys :req [::error]))

(s/def :event/event (s/multi-spec ostate-type ::ostate))
(s/valid? :event/event
          {::ostate :ostate/error
           ::error "BAM"})
(s/valid? :event/event
          {::ostate :ostate/init
           ::value 2.})
(s/valid? :event/event
          {::ostate :ostate/invalid
           ::error "BAM"})

(s/def :account/val double?)
(s/def ::account (s/keys :req [:account/val]))
(s/def ::market-state (s/map-of string? int?))
(s/def ::systems (s/map-of string? int?))
(s/def ::state (s/keys :req [::account ::market-state]
                       :opt [:event/event]))

(defn example []
  (gen/generate (s/gen ::state)))

(defn update-account-val
  [state]
  (update state ::account assoc ::value 3.))

(update-account-val (example))
(comment
  {:account {:val 11.9}
   :market-state {:iotdamxd-1 {:state "O"
                               :data [{:o 1. :h 2. :l 3. :c 4 :t java.time/now}]}}
   :systems {:system1 {:markets [:iotdamxd-1]}}})
