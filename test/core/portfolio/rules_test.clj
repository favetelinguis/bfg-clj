(ns core.portfolio.rules-test
  (:require [core.portfolio.rules :as rules]
            [core.command :as command]
            [core.events :as e]
            [ig.stream.item :as i]
            [core.signal :as signal])
  (:require
   [clojure.spec.test.alpha :as st]
   [clojure.test :refer :all]
   [odoyle.rules :as o]))

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

; BELOW ARE OLD TEST THAT I MIGHT WANT INSPIRATION FROM AND THEN DELETE
;; (defn debug-rules []
;;   (rules/init-bfg-session
;;     (->> rules/rule-set
;;          (map (fn [rule]
;;                 (o/wrap-rule rule
;;                              {:what
;;                               (fn [f session new-fact old-fact]
;;                                 (println :what (:name rule) new-fact old-fact)
;;                                 (f session new-fact old-fact))
;;                               :when
;;                               (fn [f session match]
;;                                 (println :when (:name rule) match)
;;                                 (f session match))
;;                               :then
;;                               (fn [f session match]
;;                                 (println :then (:name rule) match)
;;                                 (f session match))
;;                               :then-finally
;;                               (fn [f session]
;;                                 (println :then-finally (:name rule))
;;                                 (f session))}))))))

;; (deftest adding-ohlc-bar-and-series-test
;;   (-> (rules/init-bfg-session rules/rule-set)
;;       (o/insert :DAX ::ohlc/bar data/bar-0)
;;       o/fire-rules
;;       ((fn [session]
;;          (is (o/contains? session :DAX ::ohlc/bar))
;;          (is (= 1 (count (o/query-all session ::rules/ohlc-bar))))
;;          (is (= 1 (count (:ohlc-series (first (o/query-all session ::rules/ohlc-series))))))
;;          session))
;;       (o/insert :DAX ::ohlc/bar data/bar-1)
;;       o/fire-rules
;;       (o/insert :DAX ::ohlc/bar data/bar-2)
;;       o/fire-rules
;;       ((fn [session]
;;          (is (= 1 (count (o/query-all session ::rules/ohlc-bar))))
;;          (is (= 3 (count (:ohlc-series (first (o/query-all session ::rules/ohlc-series))))))
;;          session))
;;       ))

;; (deftest atr-series-test
;;   (with-redefs [atr/periods 3]
;;     (-> (rules/init-bfg-session rules/rule-set)
;;         (o/insert :DAX ::ohlc/bar data/bar-0)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (not (o/contains? session :DAX ::atr/series)))
;;            session))
;;         (o/insert :DAX ::ohlc/bar data/bar-1)
;;         o/fire-rules
;;         (o/insert :DAX ::ohlc/bar data/bar-2)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (o/contains? session :DAX ::atr/series))
;;            (is (= 1 (count (:atr-series (first (o/query-all session ::rules/atr-series))))))
;;            session))
;;         (o/insert :DAX ::ohlc/bar data/bar-3)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (= 2 (count (:atr-series (first (o/query-all session ::rules/atr-series))))))
;;            session))
;;         )))

;; (deftest ha-series-test
;;   (with-redefs [atr/periods 3]
;;     (-> (rules/init-bfg-session rules/rule-set)
;;         (o/insert :DAX ::ohlc/bar data/bar-0)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (o/contains? session :DAX ::ha/series))
;;            (is (empty? (:ha-series (first (o/query-all session ::rules/ha-series)))))
;;            session))
;;         (o/insert :DAX ::ohlc/bar data/bar-1)
;;         o/fire-rules
;;         (o/insert :DAX ::ohlc/bar data/bar-2)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (= 1 (count (:ha-series (first (o/query-all session ::rules/ha-series))))))
;;            session))
;;         (o/insert :DAX ::ohlc/bar data/bar-3)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (= 2 (count (:ha-series (first (o/query-all session ::rules/ha-series))))))
;;            session))
;;         )))

;; (def setup-trigger-bar
;;   (ohlc/make-bar :DAX (Instant/parse "2022-01-25T09:18:00Z") 15855. 15215. 15222. 15777.))

;; (deftest setup-test
;;   (with-redefs [atr/periods 2
;;                 signal/atr-multiple-setup-target 1
;;                 signal/consecutive-heikin-ashi-bars-trigger 2]
;;     (-> (rules/init-bfg-session rules/rule-set)
;;         ((fn [session]
;;            (is (o/contains? session :DAX ::signal/signal))
;;            (is (= :await-setup (:signal (first (o/query-all session ::rules/signal)))))
;;            session))
;;         (o/insert :DAX ::ohlc/bar data/bar-0)
;;         o/fire-rules
;;         (o/insert :DAX ::ohlc/bar data/bar-1)
;;         o/fire-rules
;;         (o/insert :DAX ::ohlc/bar data/bar-2)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (= :await-setup (:signal (first (o/query-all session ::rules/signal)))))
;;            session))
;;         (o/insert :DAX ::ohlc/bar setup-trigger-bar)
;;         o/fire-rules
;;         ((fn [session]
;;            (is (= :await-entry (:signal (first (o/query-all session ::rules/signal)))))
;;            session))
;;         )))

;; ;; test await-entry
;; ;; new bar same directn
;; ;; new bar entry bar but not doji
;; ;; new bar entry so enter
