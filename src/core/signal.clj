(ns core.signal)

(defprotocol Signal
  (on-midprice [this midprice] "Gets called on midprice")
  (on-candle [this candle] "Get called on candle"))

(deftype DummySignal [state]
  Signal
  (on-midprice [this midprice] (swap! state inc))
  (on-candle [this candle] (swap! state dec)))

(defn make-dummy-signal
  ([name]
   (make-dummy-signal name (str (java.util.UUID/randomUUID))))
  ([name id]
   {::name name
    ::id id
    ::state ::inactive
    ::runner (->DummySignal (atom {}))}))
