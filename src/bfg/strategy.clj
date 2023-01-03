(ns bfg.strategy
  (:require
    [clojure.spec.gen.alpha :as gen]
    [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::direction #{:l :s})
(s/def ::change string?)
(s/def ::decision string?)
(s/def ::step-fn (s/fspec :args (s/cat :c ::change) :ret ::decision))

(s/def ::strategy (s/keys :req [::step-fn]
                          :opt []))

(gen/generate (s/gen ::strategy))
(s/valid? ::step-fn (fn [b] "dd"))