(ns ig.cache)

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))
