(ns bfg.portfolio.position)

(defn make
  [id price time]
  {::id      id
   ::order   nil
   :price    price
   :time     time})
