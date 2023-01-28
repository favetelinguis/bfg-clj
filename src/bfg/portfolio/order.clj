(ns bfg.portfolio.order)

(defn make
  [id market-id size direction price]
  {::id        id
   ::market-id       market-id
   ::size      size
   ::direction direction
   ::price     price
   ::type :working-order
   })