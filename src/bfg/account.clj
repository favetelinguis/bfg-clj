(ns bfg.account
  (:require
    [clojure.spec.gen.alpha :as gen]
    [clojure.test.check.generators :as g]
    [bfg.strategy :as strategy]
    [clojure.spec.alpha :as s]))

(s/def ::id (s/with-gen string? #(s/gen #{"dej" "mej" "hej"})))
(s/def ::available-cash (s/double-in :min 0.0 :max 10000000.0 :NaN? false))
(s/def ::funds (s/double-in :min 0.0 :max 10000000.0 :NaN? false))
(s/def ::account (s/keys :req [::id ::available-cash ::funds]))

(defn make [id]
  {::id id ::available-cash 0.0 ::funds 0.0})

(s/fdef make
        :args (s/cat :account-id ::id)
        :ret ::account
        :fn (s/and
              #(= (-> % :args :account-id) (-> % :ret ::id))
              #(= (-> % :ret ::funds) 0.0)
              #(= (-> % :ret ::available-cash) 0.0)))

(gen/sample (gen/such-that #(= % 22) g/nat) 19993)
(gen/sample (g/recursive-gen shuffle [1 2 3 4]))