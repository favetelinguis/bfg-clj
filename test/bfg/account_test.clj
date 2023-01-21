(ns bfg.account-test
  (:require [clojure.test :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest])
  (:require [bfg.account :as account :refer :all]))

(deftest make-account
  (is
    (-> (stest/check `make)
        first
        :clojure.spec.test.check/ret
        :pass?)))

(defspec make-account2
  (prop/for-all [id (s/gen ::account/id)]
    (= {::account/available-cash 0.0
        ::account/funds          0.0
        ::account/id             id} (account/make id))
    )
  )
