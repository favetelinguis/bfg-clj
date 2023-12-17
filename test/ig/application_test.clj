(ns ig.application-test
  (:require [ig.application :as sut]
            [clojure.test :as t]))

(t/deftest app-test-market
  (let [results (atom [])
        f (fn [e] (swap! results conj e))
        {:keys [kill-app send-to-app!!]} (sut/app f)]
    (send-to-app!! {"ROUTE" "MARKET:id"})
    (Thread/sleep 100)
    (kill-app)
    (Thread/sleep 100)
    (let [[e-in-market & killed] @results]
      (t/is (= {"ROUTE" "MARKET:id"} e-in-market))
      (t/is (= 4 (->> killed
                      (filter #(= :killed %))
                      (count)))))))

(t/deftest app-test-account
  (let [results (atom [])
        f (fn [e] (swap! results conj e))
        {:keys [kill-app send-to-app!!]} (sut/app f)
        event {"ROUTE" "ACCOUNT:id" "AVAILABLE_CASH" "22.2"}]
    (send-to-app!! event)
    (Thread/sleep 100)
    (kill-app)
    (Thread/sleep 500)
    (let [[e-in-account & killed] @results]
      (t/is (= event e-in-account))
      #_(t/is (= 4 killed #_(->> killed
                                 (filter #(= :killed %))
                                 (count)))))))
