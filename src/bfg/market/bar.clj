(ns bfg.market.bar)

(defn make [market-id time high low open close]
  {::id market-id :time time :o open :h high :l low :c close})

(defn make-bar-series
  [market-id]
  {::id market-id ::bars (list)}
  )

(defn get-bars
  [bar-series]
  (get bar-series ::bars))

(defn get-periods
  [bar-series]
  (count (get-bars bar-series)))

(defn get-latest-bar
  [bar-series]
  (first (get-bars bar-series)))

(defn get-bar-id
  [b]
  (::id b))

(defn add-bar
  "Check that each bar that is added in front is newer then the old bar
  This is very sensetive, if we update with a faulty value the barseries in a market will become nil and the
  trading system should stop  trading!
  "
  [bars bar]
  (let [is-empty-bar-series (empty? bars)
        is-same-market (= (get-bar-id bar) (get-bar-id (first bars)))
        is-new-bar-oldest (if is-empty-bar-series false (> (.compareTo (:time bar) (:time (first bars))) 0))]
    (when (or is-empty-bar-series
              (and is-same-market is-new-bar-oldest))
      (conj bars bar))))

(defn add-bars
  [bs & bars]
  (assoc bs ::bars (reduce add-bar (get-bars bs) bars)))

(defn get-market-id [bar]
  (::id bar))
