(ns ig.stream.item
  (:require
   [clojure.string :as str]
   [core.events :as e]))

(defn market-item
  [epic]
  (str "MARKET:" epic))

(defn get-route
  [item]
  (when item
    (first
     (str/split item #":"))))

(defn account?
  [item]
  (= "ACCOUNT" (get-route item)))

(defn trade?
  [item]
  (= "TRADE" (get-route item)))

(defn market?
  [item]
  (= "MARKET" (get-route item)))

(defn chart?
  [item]
  (= "CHART" (get-route item)))

(defn unsubscribe?
  "This is a special event I have made to manage to instrument store to delete items"
  [item]
  (= "UNSUBSCRIBE" (get-route item)))

(defn get-name
  [item]
  (when item
    (let [parts (str/split item #":")]
      (if (unsubscribe? item)
        (nth parts 2)
        (second parts)))))

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
  "taken an item-update java object and transform it into a clojure map for all changed fields and adds epic
  contains logic to add the correct kind"
  [item-update]
  (let [item (.getItemName item-update)
        changed-fields (into {} (.getChangedFields item-update))]
    (assoc changed-fields ::e/name (get-name item))))
