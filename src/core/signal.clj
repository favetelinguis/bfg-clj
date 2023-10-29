(ns core.signal)

(defprotocol Signal
  (get-name [this] "Get the name")
  (get-commands [this] "Return events from core.events to execute from last update")
  (on-midprice [this midprice] "Gets called on midprice")
  (on-candle [this candle] "Get called on candle"))

(defrecord DaxKillerSignal [state]
  Signal
  (get-name [this] "DAX Killer")
  (get-commands [this] this)
  (on-midprice [this midprice] this)
  (on-candle [this candle] this))

#_(deftype DummySignal [state]
    Signal
    (on-midprice [this midprice] (swap! state inc))
    (on-candle [this candle] (swap! state dec)))

#_(defn make-dummy-signal
    ([name]
     (make-dummy-signal name (str (java.util.UUID/randomUUID))))
    ([name id]
     {::name name
      ::id id
      ::state ::inactive
      ::runner (->DummySignal (atom {}))}))

(defn make-dax-killer-signal
  []
  (map->DaxKillerSignal {:state {}}))
