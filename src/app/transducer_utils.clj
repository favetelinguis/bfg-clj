(ns app.transducer-utils)

(defn ex-fn [error] (println "Error: " (.getMessage error)))

(defn make-state-transducer [f start-state]
  (let [state (volatile! start-state)]
    (fn [xf]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
         (let [[xs next] (f @state input)]
           (vreset! state next)
           (if (empty? xs)
             result
             (doseq [x xs]
               (xf result x)))))))))
