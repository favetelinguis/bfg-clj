(ns ig.portfolio-test
  (:require [ig.portfolio :as sut]
            [clojure.test :as t]
            [ig.cache :as cache]
            [core.events :as e]))

(t/deftest signal-generate-order-if-no-order
  (let [change (e/signal-new "dax" "long")
        old {}
        make-event (e/create-new-order "dax" "long" 1)
        result (cache/make [make-event] {"dax" make-event})]
    (t/is (= result (sut/update-cache (cache/make old) change)))))

(t/deftest signal-same-direction-do-nothing
  (let [change (e/signal-new "dax" "long")
        old {"dax" (e/create-new-order "dax" "long" 1)}
        result (cache/make [] old)]
    (t/is (= result (sut/update-cache (cache/make old) change)))))

(t/deftest signal-other-direction-generate-oposite-order-dont-change-state
  (let [change (e/signal-new "dax" "short")
        make-event (e/create-new-order "dax" "short" 1)
        old {"dax" (e/create-new-order "dax" "long" 1)}
        result (cache/make [make-event] old)]
    (t/is (= result (sut/update-cache (cache/make old) change)))))

(t/deftest exit-order-will-do-nothing-if-no-order
  (let [change (e/exit "dax")
        old {"omx" :dontmatter}
        result (cache/make old)]
    (t/is (= result (sut/update-cache (cache/make old) change)))))

(t/deftest exit-order-will-remove-it
  (let [change (e/exit "dax")
        old {"dax" :dontmatter}
        result (cache/make)]
    (t/is (= result (sut/update-cache (cache/make old) change)))))

(t/deftest updating-balance
  (let [change (e/create-balance-event "33" 444)
        old {"dax" :dontmatter}
        result (cache/make {"dax" :dontmatter :balance 444})]
    (t/is (= result (sut/update-cache (cache/make old) change)))))
