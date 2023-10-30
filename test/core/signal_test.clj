(ns core.signal-test
  (:require [core.signal :as sut]
            [clojure.test :as t]
            [core.indicators.ohlc-series :as ohlc]
            [core.events :as e])
  (:import [java.time Instant]))

(t/deftest can-get-name
  (let [signal (sut/make-dax-killer-signal)]
    (t/is (= "DAX Killer" (sut/get-name signal)))))

(t/deftest no-bars-is-null
  (-> (sut/make-dax-killer-signal)
      (sut/on-candle (ohlc/make-empty-series))
      (sut/on-candle (ohlc/make-empty-series))
      (sut/on-candle (ohlc/make-empty-series))
      ((fn [signal]
         (println signal)
         (t/is (= 0 (:num signal)))))))

(t/deftest no-signal-is-null
  (let [series (atom (ohlc/make-empty-series))]
    (-> (sut/make-dax-killer-signal)
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 90)))
        ((fn [signal]
           (t/is (= 0 (:num signal)))
           (t/is (nil? (sut/get-commands signal "epic"))))))))

(t/deftest two-up-generate-buy
  (let [series (atom (ohlc/make-empty-series))]
    (-> (sut/make-dax-killer-signal)
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 90)))
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 95)))
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 100)))
        ((fn [signal]
           (t/is (= 2 (:num signal)))
           (t/is (= :buy (::e/direction (sut/get-commands signal "epic")))))))))

(t/deftest two-down-generate-sell
  (let [series (atom (ohlc/make-empty-series))]
    (-> (sut/make-dax-killer-signal)
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 90)))
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 80)))
        (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 70)))
        ((fn [signal]
           (t/is (= -2 (:num signal)))
           (t/is (= :sell (::e/direction (sut/get-commands signal "epic")))))))))

(t/deftest signal-will-change
  (let [series (atom (ohlc/make-empty-series))]
    (->
     (sut/make-dax-killer-signal)
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 90)))
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 80)))
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 70)))
     ((fn [signal]
        (t/is (= -2 (:num signal)))
        (t/is (= :sell (::e/direction (sut/get-commands signal "epic"))))
        signal))
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 80)))
     ((fn [signal]
        (t/is (= 1 (:num signal)))
        signal))
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 90)))
     (sut/on-candle (swap! series ohlc/add-bar (e/create-candle-event "dax" (Instant/now) 100 10 50 100)))
     ((fn [signal]
        (t/is (= 3 (:num signal)))
        (t/is (= :buy (::e/direction (sut/get-commands signal "epic")))))))))
