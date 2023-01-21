(ns bfg.context-test
  (:require [clojure.test :refer :all])
  (:require [bfg.bfg_context :as sut])
  (:import (java.time Duration Instant)
           (java.time.temporal ChronoUnit)))

(def bar1
  (let [now (Instant/now)
        o 1.9
        h 2.2
        l 1.1
        c 2.1
        v 2222
        ]
    (sut/make-bar now o h l c v)
    {:time   now
     :open   o
     :high   h
     :low    l
     :close  c
     :volume v
     }))

(def bar2 (update bar1 :time (fn [i] (.plus i 3 ChronoUnit/MINUTES))))

(def test-series (sut/add-bars (sut/make-bar-series (Duration/ofMinutes 3)) bar1 bar2))

(deftest bar-test
  (let [now (Instant/now)
        o 1.9
        h 2.2
        l 1.1
        c 2.1
        v 2222
        ]
    (is (= (sut/make-bar now o h l c v)
           {:time   now
            :open   o
            :high   h
            :low    l
            :close  c
            :volume v
            }))))

(deftest bar-series-test
  (let [d (Duration/ofMinutes 3)
        new-bar-series (sut/make-bar-series d)]
    (is (= new-bar-series {:bars '() :duration d}))
    (is (= (count (:bars (sut/add-bar new-bar-series bar1))) 1))
    (is (= (sut/add-bars new-bar-series bar2 bar1) nil))))

(deftest indicator-test
  (is (= 2.1 ((sut/last-price :close) test-series)))
  (is (= 4.2 ((sut/sa 2 :close) test-series))))

(deftest add-to-world-test
  (is (= 1 (count (:markets (sut/add-market (sut/make-world) (sut/make-market :DAX nil nil))))))
  (is (= 1 (count (:accounts (sut/add-account (sut/make-world) (sut/make-account :test 199.2 21111)))))))