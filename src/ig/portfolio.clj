(ns ig.portfolio)

;; This cache is part of the stateful transducers it must have the function signature
;; (fn [old-cache event] [[::e/events to propagate] updated-cache])

(defn make
  ([]
   (make {}))
  ([initial-cache]
   (make [] initial-cache))
  ([events initial-cache]
   [events initial-cache]))

(defn update-signal
  "Signal is the only that generate out event to create order"
  [old change]
  (let [strategy-name (:signal change)]
    (make (update old strategy-name merge change))))

(defn update-cache [old event]
  (let []
    (cond
      ; account
      ; strategy signal
      ; order acc
      :else (println "Unsupported event: " event))))
