(ns bfg.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/conform even? 121)
(s/valid? even? 121)
(s/valid? #{121 11 2} 12)

(s/def ::order even?)
(s/valid? ::order 22)

(s/merge)
(gen/sample (s/gen int?))

(s/def ::tick (s/double-in :min -0.1 :max 0.1 :NaN? false :infinite? false))
(gen/sample (s/gen ::tick))
;; Spin up a separate thread that generate data at fixed time to work as mock for
;; how to use gen/bind

(defn adder [] (+ 1 1))