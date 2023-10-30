(ns core.portfolio.rules-test
  (:require [core.portfolio.rules :as rules]
            [core.command :as command]
            [core.events :as e]
            [ig.stream.item :as i]
            [core.command :refer [CommandExecutor]]
            [core.signal :as signal])
  (:require
   [clojure.spec.test.alpha :as st]
   [clojure.test :refer :all]
   [odoyle.rules :as o])
  (:import [java.time Instant]))

(deftype TestCommandExecutor [state]
  CommandExecutor
  (open-order! [this order] (swap! state conj order))
  (close-order! [this order] (swap! state conj order)))

(defn debug-rules []
  (->> rules/rules
       (map (fn [rule]
              (o/wrap-rule rule
                           {:what
                            (fn [f session new-fact old-fact]
                              (println :what (:name rule) new-fact old-fact)
                              (f session new-fact old-fact))
                            :when
                            (fn [f session match]
                              (println :when (:name rule) match)
                              (f session match))
                            :then
                            (fn [f session match]
                              (println :then (:name rule) match)
                              (f session match))
                            :then-finally
                            (fn [f session]
                              (println :then-finally (:name rule))
                              (f session))})))))

(deftest updating-facts
  (-> (reduce o/add-rule (o/->session)
              (o/ruleset
               {::updating-facts
                [:what
                 [y ::right b]]}))
      (#(reduce (fn [session {:keys [::id] :as vals}] (o/insert session id vals)) % [{::id ::bob ::right "blue"} {::id ::bob ::right "blue"}]))
      (o/insert ::bob {::id ::bob ::right "blue"})
      (o/insert ::yair {::id ::yair ::right ::zach})
      o/fire-rules
      ((fn [session]
         (is (= 4 (count (o/query-all session ::updating-facts))))
         session))))
#_(st/instrument)

(deftest signals-can-get-activated
  (-> (rules/create-session (command/->DummyCommandExecutor) [(signal/make-dax-killer-signal)] :id-fn #(identity "test-id"))
      (rules/activate-signal-for-market "test-id" "dax")
      ((fn [session]
         (is (= 1 (filter ::signal/active? (rules/get-all-signals session))))))))

(deftest sell-order-is-triggered-only-once
  (let [result (atom [])
        epic "dax"
        signal-id "test-id"]
    (-> (rules/create-session (->TestCommandExecutor result)
                              [(signal/make-dax-killer-signal)]
                              :id-fn #(identity signal-id))
        (rules/activate-signal-for-market signal-id epic)
        (rules/subscribe-market epic)
        (rules/update-session (e/create-balance-event "account1" 100000))
        (rules/update-session (e/create-candle-event epic (Instant/now) 100 10 60 70))
        (rules/update-session (e/create-candle-event epic (Instant/now) 100 10 60 60))
        (rules/update-session (e/create-candle-event epic (Instant/now) 100 10 60 50))
        (rules/update-session (e/create-candle-event epic (Instant/now) 100 10 60 40))
        (rules/update-session (e/create-candle-event epic (Instant/now) 100 10 60 30))
        ((fn [session]
           (is (= 1 (count @result))))))))

