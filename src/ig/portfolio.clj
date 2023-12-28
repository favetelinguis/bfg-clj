(ns ig.portfolio
  (:require [ig.cache :as cache]
            [core.events :as e]
            [ig.stream.item :as i]))

(defn- position-size
  "TODO imp whatever postion size I want to use"
  [old change]
  1)

(defn update-signal
  "Make order if no order exist, only one order/market even if multiple strategies exist.
  If order exist do nothing, if signal is in other direction send.
  TODO can get inconsistent, if we close an order here and it fail in command executor we wont know"
  [[_ old] change]
  (let [id (::e/name change) ; TODO not all event have name?
        has-order? (not (nil? (get old id)))
        make-new-order (fn [x update-state?]
                         (let [e (e/open-order
                                  id
                                  x
                                  (position-size old change))]
                           (cache/make
                            [e]
                            (if update-state?
                              (update old id merge e)
                              old))))]
    (if-not has-order?
      (make-new-order (::e/direction change) true)
      (let [old-direction (get-in old [id ::e/direction])
            new-direction (::e/direction change)
            same-direction? (= old-direction new-direction)]
        (if-not same-direction?
          (make-new-order new-direction false) ; Will have to change when more advanced position-sizing
          (cache/make old))))))

(defn update-on-exit
  [[_ old] change]
  (let [id (::e/name change)
        result (dissoc old id)]
    (cache/make result)))

(defn update-balance
  [[_ old] change]
  (let [new-balance (::e/balance change)
        result (assoc old :balance new-balance)]
    (cache/make result)))

(defn update-cache [old event]
  (case (::e/kind event)
    ::e/signal (update-signal old event)
    ::e/balance (update-balance old event)
    ::e/exit (update-on-exit old event)
    :else (println "Unsupported event: " event)))
