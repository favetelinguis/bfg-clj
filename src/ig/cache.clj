(ns ig.cache)

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn combine
  [[_ c1] [_ c2]]
  (make (merge c1 c2)))
