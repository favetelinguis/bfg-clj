(ns bfg.market.signal-test
  (:require [clojure.test :refer :all])
  (:require
    [bfg.data-test :as test-data]
    [bfg.market.indicators.heikin-ashi :as ha]
    [bfg.market.signal :as sut]))

(deftest setup-test
  (let [test-state (-> test-data/test-signal
                       (sut/step-signal test-data/bar-0)
                       (sut/step-signal test-data/bar-1)
                       (sut/step-signal test-data/bar-2)
                       (sut/step-signal test-data/bar-3)
                       )]
    (is (= (sut/get-first test-state) {:c 15091.3 :h 15116.5 :l 15033.319497680664 :o 15033.319497680664}))
    (is (= (sut/get-first-atr test-state) 52.8999999999999))
    (is (= (sut/get-count-same-direction test-state) 4))
    (is (= (ha/calculate-bar-direction (ha/get-previous (sut/get-heikin-ashi-state test-state))) :UP)))
  )

(deftest entry-test
  (let [test-state (-> test-data/test-signal
                       (sut/step-signal test-data/bar-0)
                       (sut/step-signal test-data/bar-1)
                       (sut/step-signal test-data/bar-2)
                       (sut/step-signal test-data/bar-3)
                       )]
    (is (= (sut/get-last
             (-> test-state
                 (sut/update-state :await-entry)
                 (sut/step-signal test-data/bar-0)
                 )) {:c 15157.225 :h 15163.1 :l 15141.53246860504 :o 15141.53246860504}))
    (is (= (sut/get-entry
             (-> test-state
                 (sut/update-state :await-entry)
                 (sut/step-signal test-data/bar-0)
                 )) {:c 15133.35 :h 15149.378734302521 :l 15130.7 :o 15149.378734302521})))
  )

(deftest exit-test
  (let [test-state (-> test-data/test-signal
                       (sut/step-signal test-data/bar-0)
                       (sut/step-signal test-data/bar-1)
                       (sut/step-signal test-data/bar-2)
                       (sut/step-signal test-data/bar-3)
                       )]
    (is (= (sut/get-state
             (-> test-state
                 (sut/update-state :await-exit)
                 (sut/step-signal test-data/bar-0)
                 )) :await-setup))
    ))