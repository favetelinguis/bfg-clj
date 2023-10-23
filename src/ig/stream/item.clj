(ns ig.stream.item
  (:require
   [ig.market-cache :as market-cache]
   [meander.epsilon :as m]
   [clojure.string :as str]
   [clojure.spec.alpha :as s]
   [core.events :as e]))

(defn market-item
  [epic]
  (str "MARKET:" epic))

(defn get-name
  [item]
  (when item
    (second
     (str/split item #":"))))

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

(defn into-map
  "taken an item-update java object and transform it into a clojure map for all changed fields and adds epic"
  [item-update]
  (let [name (get-name (.getItemName item-update))
        changed-fields (into {} (.getChangedFields item-update))]
    (assoc changed-fields "NAME" name)))

(defn market-item-update->market-update
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
  [item-update]
  (let [epic (second (str/split (.getItemName item-update) #":"))
        changed-fields (into {} (.getChangedFields item-update))]
    (m/match changed-fields

                 {"UPDATE_TIME" ?UPDATE_TIME
                  "MARKET_DELAY" ?MARKET_DELAY
                  "MARKET_STATE" ?MARKET_STATE
                  "BID" ?BID
                  "OFFER" ?OFFER}

                 {::market-cache/update-time ?UPDATE_TIME
                  ::market-cache/market-delay ?MARKET_DELAY
                  ::market-cache/market-state ?MARKET_STATE
                  ::market-cache/bid ?BID ; we can delete this and only use candle for price
                  ::market-cache/offer ?OFFER ; we can delete this and only use candle for price
                  ::market-cache/epic epic})))
