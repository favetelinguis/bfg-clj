(ns bfg-ig.stream.item
(:require
   [bfg.market :as market]
   [meander.epsilon :as m]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [clojure.spec.alpha :as s]
   [bfg.error :as error])
  )

(defn market-item
  [epic]
  (str "MARKET:" epic))

(defn get-epic
  [item]
  (second
   (str/split item #":")))

(defn trade-item
  [account-id]
  (str "TRADE:" account-id))

(defn account-item
  [account-id]
  (str "ACCOUNT:" account-id))

(defn chart-candle-item
  "Possible scale SECOND, 1MINUTE, 5MINUTE, HOUR"
  [scale epic]
  (str/join ":" ["CHART" epic scale]))

(def chart-candle-1min-item
  (partial chart-candle-item "1MINUTE"))


(defn market-item-update->bfg-market-update-event
  "Ensure that incomming data conforms to event structure.
  Throws exceptions need to catch Throwable both :post and m/match throws exceptions
  and are not catched in this function
  {:type :market-update,
  :epic IX.D.DAX.IFMM.IP,
  :update-time 17:29:51, TODO convert to date
  :market-delay 0, TODO convert to boolean
  :market-state TRADEABLE,
  :bid 15109.8, TODO convert to double
  :offer 15112.6} TODO convert to double
  TODO write tests for this fn
  "
  ;; TODO maybe we need an error type to use or how should i handle exeptions?
  [item-update]
  (try
       (let [epic (second (str/split (.getItemName item-update) #":"))
             changed-fields (into {} (.getChangedFields item-update)) ; return an immutable java map so convert it to someting Clojure
             result (m/match changed-fields

               {"UPDATE_TIME" ?UPDATE_TIME
                "MARKET_DELAY" ?MARKET_DELAY
                "MARKET_STATE" ?MARKET_STATE
                "BID" ?BID
                "OFFER" ?OFFER}

                ; TODO is this really the only way to refere to a namespaced keyword?
               {:bfg.market/update-time ?UPDATE_TIME
                :bfg.market/market-delay ?MARKET_DELAY
                :bfg.market/market-state ?MARKET_STATE
                :bfg.market/bid ?BID ; we can delete this and only use candle for price
                :bfg.market/offer ?OFFER ; we can delete this and only use candle for price
                :bfg.market/type :bfg.market/market-update
                :bfg.market/epic epic})]
         (if (s/valid? :bfg.market/event result)
           result
          (error/create-fatal-error (str "Invalid market update: " result))))
       (catch Throwable e (error/create-fatal-error (ex-message e)))))
