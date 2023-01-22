(ns bfg.bfg-context-test
  (:require [clojure.test :refer :all])
  (:require [bfg.bfg_context :as sut])
  (:import (java.time Duration Instant)
           (java.time.temporal ChronoUnit)))

(defn test-position-sizing-strategy
  [portfolio accounts decisions]
  ["TODO Return list delta-portfolio-commands"])

(def bar1
  (let [now (Instant/now)
        o 1.9
        h 2.2
        l 1.1
        c 2.1
        v 2222
        id :DAX
        ]
    (sut/make-bar id now o h l c v)
    {:time   now
     :id     id
     :open   o
     :high   h
     :low    l
     :close  c
     :volume v
     }))

(def bar2 (update bar1 :time (fn [i] (.plus i 3 ChronoUnit/MINUTES))))
(def bar3 (update bar2 :time (fn [i] (.plus i 3 ChronoUnit/MINUTES))))

(def test-series (sut/add-bars (sut/make-bar-series :DAX (Duration/ofMinutes 3)) bar1 bar2))

(def test-strategy
  (-> (sut/make-strategy "Example strategy")
      (update :buy-signal conj (sut/stupid-strategy-1 :BUY))
      (update :sell-signal conj (sut/stupid-strategy-2 :SELL))))

(def test-price-update {:id :DAX :price 22.1})

(def test-market
  (sut/make-market :DAX (list test-strategy) test-series 33.2))

(def test-account
  (sut/make-account :DAX 11.11 22.12))

(def test-context
  (-> (sut/make-context test-position-sizing-strategy)
      (sut/update [:market test-market])
      (sut/update [:account test-account])))

(deftest bar-test
  (let [now (Instant/now)
        o 1.9
        h 2.2
        l 1.1
        c 2.1
        v 2222
        id :DAX
        ]
    (is (= (sut/make-bar id now o h l c v)
           {:time   now
            :id id
            :open   o
            :high   h
            :low    l
            :close  c
            :volume v
            }))))

(deftest bar-series-test
  (let [d (Duration/ofMinutes 3)
        new-bar-series (sut/make-bar-series :DAX d)]
    (is (= new-bar-series {:id :DAX :bars '() :duration d}))
    (is (= (count (:bars (sut/add-bar new-bar-series bar1))) 1))
    (is (= (sut/add-bars new-bar-series bar2 bar1) nil))))

(deftest indicator-test
  (is (= 2.1 ((sut/last-price :close) test-series)))
  (is (= 4.2 ((sut/sa 2 :close) test-series))))

(deftest run-strategy-test
  (is (= {:buy nil
          :id   "Example strategy"
          :sell true}
         (sut/run-strategy test-series test-price-update test-strategy))))

(deftest simple-context-update-test
  (is (= 2 (count (:markets (-> (sut/make-context test-position-sizing-strategy)
                                (sut/update [:market test-market])
                                (sut/update [:market (sut/make-market :SAX (list test-strategy) test-series 33.2)]))))))
  (is (= 1 (count (:accounts (-> (sut/make-context test-position-sizing-strategy)
                              (sut/update [:account (sut/make-account :DAX 333.33 33.2)]))))))
  (is (= 0 (count (:accounts (-> (sut/make-context test-position-sizing-strategy)
                                 (sut/update [:invalid (sut/make-account :DAX 333.33 33.2)]))))))
  (is (= 3 (count (get-in (-> (sut/make-context test-position-sizing-strategy)
                              (sut/update [:market test-market])
                              (sut/update [:bar bar3])) [:markets :DAX :bar-series :bars]))))
  )

;; TODO need to be able to update portfolio
;; How to model portfolio with orders and positions?
;; Test update-decisions seems not working
;;
;; strategy -> decisions -> delta-portfolio-commands
(deftest price-update-test
  (is (= 1 (-> test-context
               (sut/update [:price {:id :DAX :price 22.2}])))))