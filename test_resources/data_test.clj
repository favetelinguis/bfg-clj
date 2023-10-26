(ns data-test
  (:require [clojure.test :refer :all])
  (:require
   [core.indicators.ohlc-series :as ohlc])
  (:import (java.time ZoneId ZonedDateTime)))

(defn- t
  "Create an instant at the specified time when I collected the following data"
  [minute]
  (.toInstant (ZonedDateTime/of 2022 1 25 10 minute 0 0 (ZoneId/of "Europe/Stockholm"))))

(def dax-bar-series-14
  "This testdata is collected ons jan 25 9:00 as hourly data and forward, however there I will represent it as minute data"
  (as-> (ohlc/make-empty-series) $
    (apply ohlc/add-ohlc-bar $
           (map-indexed (fn [idx v] (ohlc/make-bar :DAX (t (+ idx 1)) (:h v) (:l v) (:o v) (:c v)))
                        [{:h 15116.5 :l 15069.0 :o 15089.0, :c 15090.7}
                         {:h 15092.2 :l 14995.7 :o 15090.2 :c 14999.2}
                         {:h 15038.7 :l 14964.2 :o 14998.2 :c 15028.7}
                         {:h 15064.7 :l 15022.7 :o 15029.2 :c 15040.2}
                         {:h 15046.2 :l 15022.2 :o 15039.7 :c 15025.7}
                         {:h 15032.9 :l 15008.9 :o 15026.2 :c 15023.4}
                         {:h 15060.4 :l 15018.9 :o 15022.9 :c 15037.4}
                         {:h 15066.1 :l 15011.1 :o 15036.4 :c 15046.6}
                         {:h 15110.1 :l 15043.6 :o 15046.1 :c 15101.1}
                         {:h 15105.1 :l 15086.6 :o 15102.1 :c 15100.6}
                         {:h 15137.1 :l 15098.1 :o 15101.1 :c 15123.6}
                         {:h 15145.1 :l 15115.1 :o 15124.1 :c 15139.6}
                         {:h 15147.1 :l 15131.1 :o 15140.1 :c 15146.6}
                         {:h 15161.5 :l 15129.2 :o 15146.1 :c 15133.5}]))))

;; Newest bar in dax-bar-series
(def newest-dax-14 (ohlc/make-bar :DAX (t 14) 15161.5 15129.2 15146.1 15133.5))
;; The following bars are following from the initial dax bar series and can be used as bar updates
(def bar-0 (ohlc/make-bar :DAX (t 15) 15135.0 15130.7 15132.8 15134.9))
(def bar-1 (ohlc/make-bar :DAX (t 16) 15159.8 15130.4 15137.2 15155.8))
(def bar-2 (ohlc/make-bar :DAX (t 17) 15175.1 15152.8 15156.5 15160.6))
(def bar-3 (ohlc/make-bar :DAX (t 18) 15163.1 15147.1 15160.1 15158.6))
(def bar-4 (ohlc/make-bar :DAX (t 19) 15169.6 15157.1 15158.1 15159.1))
(def bar-5 (ohlc/make-bar :DAX (t 20) 15164.6 15157.6 15157.6 15162.1))
(def bar-6 (ohlc/make-bar :DAX (t 21) 15168.1 15162.1 15162.6 15168.1))
(def bar-7 (ohlc/make-bar :DAX (t 22) 15190.6 15165.6 15167.6 15190.6))
(def bar-8 (ohlc/make-bar :DAX (t 23) 15221.6 15180.1 15189.6 15183.6))

;(def test-signal
;  (signal/make {
;                       :bar-series dax-bar-series-14
;                       :atr-multiple-setup-target 3
;                       :consecutive-heikin-ashi-bars-trigger 4
;                       }))

;(def test-market
;  (market/make :DAX test-signal))
;
;(def test-account
;  (account/make :CFD-DEMO 100000 100000))
;
;(def test-system
;  (system/make-system  test-account))

;; Below are the actual results from 1g for the test data above
(def dax-ha-test-data-14
  (list
   {:h 15116.5 :l 15069.0 :o 15074.3, :c 15091.3}          ;ons jan 25 9:00
   {:h 15092.2 :l 14995.7 :o 15082.8, :c 15044.3}          ;ons jan 25 10:00
   {:h 15063.6 :l 14964.2 :o 15063.6, :c 15007.5}          ;ons jan 25 11:00
   {:h 15064.7 :l 15022.7 :o 15035.5, :c 15039.2}          ;ons jan 25 12:00
   {:h 15046.2 :l 15022.2 :o 15037.4, :c 15033.5}          ;ons jan 25 13:00
   {:h 15035.4 :l 15008.9 :o 15035.4, :c 15022.9}          ;ons jan 25 14:00
   {:h 15060.4 :l 15018.9 :o 15029.1, :c 15034.9}          ;ons jan 25 15:00
   {:h 15066.1 :l 15011.1 :o 15032.0, :c 15040.0}          ;ons jan 25 16:00
   {:h 15110.1 :l 15036.0 :o 15036.0, :c 15075.2}          ;ons jan 25 17:00
   {:h 15105.1 :l 15055.6 :o 15055.6, :c 15098.6}          ;ons jan 25 18:00
   {:h 15137.1 :l 15077.1 :o 15077.1, :c 15115.0}          ;ons jan 25 19:00
   {:h 15145.1 :l 15096.0 :o 15096.0, :c 15131.0}          ;ons jan 25 20:00
   {:h 15147.1 :l 15113.5 :o 15113.5, :c 15141.2}          ;ons jan 25 21:00
   {:h 15161.5 :l 15127.4 :o 15127.4, :c 15142.6}          ;ons jan 25 22:00
   ))

(def dax-atr-test-data
  (list
   30.16738                                                ;ons jan 25 9:00
   30.90542                                                ;ons jan 25 10:00
   37.73360                                                ;ons jan 25 11:00
   37.95067                                                ;ons jan 25 12:00
   36.95420                                                ;ons jan 25 13:00
   36.02890                                                ;ons jan 25 14:00
   36.41969                                                ;ons jan 25 15:00
   37.74685                                                ;ons jan 25 16:00
   39.80065                                                ;ons jan 25 17:00
   38.27918                                                ;ons jan 25 18:00
   38.33066                                                ;ons jan 25 19:00
   37.73562                                                ;ons jan 25 20:00
   36.18307                                                ;ons jan 25 21:00
   35.90571                                                ;ons jan 25 22:00
    ; END INITIAL 14
   33.64816                                                ;ons jan 25 23:00
   33.34472                                                ;ons jan 26 00:00
   32.55581                                                ;ons jan 26 01:00
   31.37325                                                ;ons jan 26 02:00
   30.02516                                                ;ons jan 26 03:00
   28.38051                                                ;ons jan 26 04:00
   26.78190                                                ;ons jan 26 05:00
   26.65462                                                ;ons jan 26 06:00
   27.71501                                                ;ons jan 26 07:00
   ))