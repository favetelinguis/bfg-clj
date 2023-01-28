(ns bfg.account)

(defn make
  [id total available]
  {::id id ::total total ::available available})

(defn update-account
  [a update]
  (merge a update))

(defn get-total [a]
  (::total a))

(defn get-available [a]
  (::available a))
