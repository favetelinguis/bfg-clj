(ns bfg.account-test
  (:require [clojure.test :refer :all])
  (:require [bfg.account :as sut]))

(deftest make-account
         (is (= 2222 (sut/get-available
                     (-> (sut/make :demo 199 11)
                         (sut/update-account (sut/make nil nil 2222))))))
         )
