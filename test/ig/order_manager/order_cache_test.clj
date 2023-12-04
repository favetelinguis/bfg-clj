(ns ig.order-manager.order-cache-test
  (:require [ig.order-manager.order-cache :as sut]
            [clojure.test :as t]
            [core.events :as e]))

; Get a NewOrder command
; Check cache if any order exist
; If not add in cache if exist just ignore
; If not in cache make rest call and in callback update cache if other then 200 response if 200 respone all i ok
;
(t/deftest has-order?
  (t/is (not (sut/has-order? (sut/make) "dax")))
  (t/is (-> (sut/make)
            (sut/add-if-missing (e/create-new-order "dax" :buy 1))
            (sut/has-order? "dax"))))

(t/deftest new-order-updates-cache
  (-> (sut/make)
      (sut/update-cache (e/create-new-order "dax" :buy 1))
      ((fn [c]
         (t/is (= 1 (get-in c ["dax" ::e/size])))
         c))
      (sut/update-cache (e/create-new-order "dax" :buy 3))
      ((fn [c]
         (t/is (= 3 (get-in c ["dax" ::e/size])))
         c))))

(t/deftest unsupported-event-leave-cache-uncanged
  (-> (sut/make)
      (sut/update-cache (e/create-balance-event "acc" 333))
      ((fn [c]
         (t/is (= (sut/make) c))
         c))))
