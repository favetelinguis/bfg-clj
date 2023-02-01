(ns bfg.system
  (:require
    [bfg.portfolio.portfolio :as portfolio]
    [bfg.market.indicators.ohlc-series :as ohlc]
    [bfg.account :as account]
    [bfg.market.market :as market]
    [bfg.position-size :as position-size]
    ))

(defn make-system
  [account]
  {::markets         {}
   ::account         account
   ::portfolio        (portfolio/make)
   ::position-sizing (position-size/constant-size-strategy 1)
   })

(defn get-account
  [s]
  (::account s))

(defn get-market
  [s market-id]
  (get-in s [::markets market-id]))

(defn get-portfolio
  [s]
  (get s ::portfolio))

(defn add-market
  [s market]
  (update s ::markets assoc (::market/id market) market))

(defn update-account
  "account-update is an account"
  [s account-update]
  (update s ::account account/update-account account-update))

(defn run-position-sizing
  [s market-id]
  ((get s ::position-sizing)
   (get-portfolio s)
   (get-account s)
   (get-market s market-id)))

(defn update-portfolio
  [s portfolio-update]
  (case (:type portfolio-update)
    :order-created (update s ::portfolio portfolio/add-order portfolio-update)
    :order-canceled (update s ::portfolio portfolio/cancel-order portfolio-update)
    :position-opened (update s ::portfolio portfolio/order->position portfolio-update)
    :position-exited (update s ::portfolio portfolio/exit-position portfolio-update)))

(defn update-market
  [s bar]
  (update-in s [::markets (ohlc/market-id bar)] market/update-signal bar))

(defn step-system
  "Should update signal"
  [s bar]
  (let [new-system (update-market s bar)
        orders (run-position-sizing new-system (ohlc/market-id bar))]
    [new-system orders]))
