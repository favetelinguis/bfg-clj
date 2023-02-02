(ns bfg-ig.rules-test
  (:require [bfg-ig.rules :as rules]
            [bfg.signal :as signal]
            [bfg.indicators.atr-series :as atr]
            [bfg.indicators.heikin-ashi-series :as ha]
            [bfg.indicators.ohlc-series :as ohlc])
  (:require
    [bfg.data-test :as data]
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer :all]
    [odoyle.rules :as o])
  (:import (java.time Instant)))

(st/instrument)

(defn debug-rules []
  (rules/init-bfg-session
    (->> rules/rule-set
         (map (fn [rule]
                (o/wrap-rule rule
                             {:what
                              (fn [f session new-fact old-fact]
                                (println :what (:name rule) new-fact old-fact)
                                (f session new-fact old-fact))
                              :when
                              (fn [f session match]
                                (println :when (:name rule) match)
                                (f session match))
                              :then
                              (fn [f session match]
                                (println :then (:name rule) match)
                                (f session match))
                              :then-finally
                              (fn [f session]
                                (println :then-finally (:name rule))
                                (f session))}))))))

;(deftest adding-ohlc-bar-and-series-test
;  (-> (rules/init-bfg-session)
;      (o/insert :DAX ::ohlc/bar data/bar-0)
;      o/fire-rules
;      ((fn [session]
;         (is (o/contains? session :DAX ::ohlc/bar))
;         (is (= 1 (count (o/query-all session ::rules/ohlc-bar))))
;         (is (= 1 (count (:ohlc-series (first (o/query-all session ::rules/ohlc-series))))))
;         session))
;      (o/insert :DAX ::ohlc/bar data/bar-1)
;      o/fire-rules
;      (o/insert :DAX ::ohlc/bar data/bar-2)
;      o/fire-rules
;      ((fn [session]
;         (is (= 1 (count (o/query-all session ::rules/ohlc-bar))))
;         (is (= 3 (count (:ohlc-series (first (o/query-all session ::rules/ohlc-series))))))
;         session))
;      ))
;
;(deftest atr-series-test
;  (with-redefs [atr/periods 3]
;    (-> (rules/init-bfg-session)
;        (o/insert :DAX ::ohlc/bar data/bar-0)
;        o/fire-rules
;        ((fn [session]
;           (is (not (o/contains? session :DAX ::atr/series)))
;           session))
;        (o/insert :DAX ::ohlc/bar data/bar-1)
;        o/fire-rules
;        (o/insert :DAX ::ohlc/bar data/bar-2)
;        o/fire-rules
;        ((fn [session]
;           (is (o/contains? session :DAX ::atr/series))
;           (is (= 1 (count (:atr-series (first (o/query-all session ::rules/atr-series))))))
;           session))
;        (o/insert :DAX ::ohlc/bar data/bar-3)
;        o/fire-rules
;        ((fn [session]
;           (is (= 2 (count (:atr-series (first (o/query-all session ::rules/atr-series))))))
;           session))
;        )))
;
;(deftest ha-series-test
;  (with-redefs [atr/periods 3]
;    (-> (rules/init-bfg-session)
;        (o/insert :DAX ::ohlc/bar data/bar-0)
;        o/fire-rules
;        ((fn [session]
;           (is (o/contains? session :DAX ::ha/series))
;           (is (empty? (:ha-series (first (o/query-all session ::rules/ha-series)))))
;           session))
;        (o/insert :DAX ::ohlc/bar data/bar-1)
;        o/fire-rules
;        (o/insert :DAX ::ohlc/bar data/bar-2)
;        o/fire-rules
;        ((fn [session]
;           (is (= 1 (count (:ha-series (first (o/query-all session ::rules/ha-series))))))
;           session))
;        (o/insert :DAX ::ohlc/bar data/bar-3)
;        o/fire-rules
;        ((fn [session]
;           (is (= 2 (count (:ha-series (first (o/query-all session ::rules/ha-series))))))
;           session))
;        )))

(def setup-trigger-bar
  (ohlc/make-bar :DAX (Instant/parse "2022-01-25T09:18:00Z") 15855. 15215. 15222. 15777.))

(deftest setup-test
  (with-redefs [atr/periods 2
                signal/atr-multiple-setup-target 1
                signal/consecutive-heikin-ashi-bars-trigger 2]
    (-> (rules/init-bfg-session rules/rule-set)
        ((fn [session]
           (is (o/contains? session :DAX ::signal/signal))
           (is (= :await-setup (:signal (first (o/query-all session ::rules/signal)))))
           session))
        (o/insert :DAX ::ohlc/bar data/bar-0)
        o/fire-rules
        (o/insert :DAX ::ohlc/bar data/bar-1)
        o/fire-rules
        (o/insert :DAX ::ohlc/bar data/bar-2)
        o/fire-rules
        ((fn [session]
           (is (= :await-setup (:signal (first (o/query-all session ::rules/signal)))))
           session))
        (o/insert :DAX ::ohlc/bar setup-trigger-bar)
        o/fire-rules
        ((fn [session]
           (is (= :await-entry (:signal (first (o/query-all session ::rules/signal)))))
           session))
        )))