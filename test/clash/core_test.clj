;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.core_test
  (:require [clash.text_tools :as tt])
  (:use [clojure.test]
        [clash.core]
        [clash.command_test]
        [clash.tools]) )

;; For more examples, see stock_example_test

;; Medium complexity structures
(def medium_complexity
  '({:foo "FOO" :bar {:zoo "ZOO" :fur (2 4)} }
     {:foo "BAR" :bar {:zoo "ZAP" :fur (3 5 7)} }) )

(defn is-zoo?
  [stock]
  (fn [solution] (= stock (-> solution :bar :zoo))) )

(def is-fur-odd?
  (fn [solution]
    (let [values (-> solution :bar :fur)]
      (every? odd? values)) ) )

(deftest test-count-with-conditions__medium_complexity
  (are [x y] (= x y)
    true ((is-zoo? "ZOO") (first medium_complexity))
    0 (s-count-with medium_complexity (is-zoo? "PIG"))
    1 (s-count-with medium_complexity (is-zoo? "ZOO"))
    1 (s-count-with medium_complexity is-fur-odd?)
    0 (s-count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "BAR")))
    1 (s-count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "ZAP")))
    ) )

(def foo-numbers '(2 3 4 5 9 11 12 15 20 21 25 26 27))
(def foo-numbers-mixed '(2 3 4 5 9 "a" 11 12 15 20 21 "b" 25 26 27))

(deftest test-count-with
  (let [r1 (s-count-with foo-numbers-mixed (all? number?))
        r2 (s-count-with foo-numbers-mixed (all? number? even?))
        r3 (s-count-with foo-numbers-mixed (all? number? even?) 37)]

    (are [x y] (= x y)
      13 r1
      5  r2
      42 r3
      ) ) )

(deftest test-pcount-with
  (let [r1 (p-count-with foo-numbers-mixed (all? number?))
        r2 (p-count-with (into [] foo-numbers-mixed) (all? number?))
        r3 (p-count-with foo-numbers-mixed (all? number? even?))
        r4 (p-count-with (into [] foo-numbers-mixed) (all? number? even?))
        r5 (p-count-with (into [] foo-numbers-mixed) (all? number? even?) 37)]

    (are [x y] (= x y)
      13 r1
      13 r2
      5 r3
      5 r4
      42 r5
      ) ) )

(deftest test-collect-with
  (let [r1 (collect-with foo-numbers-mixed (all? number?) :plevel 1)
        r2 (collect-with foo-numbers-mixed (all? number? even?) :plevel 1)]

    (are [x y] (= x y)
      13 (count r1)
      5 (count r2)
      ) ) )

(deftest test-pcollect-with
  (let [r1 (collect-with foo-numbers-mixed (all? number?))
        r2 (collect-with (into [] foo-numbers-mixed) (all? number?))
        r3 (collect-with (into [] foo-numbers-mixed) (all? number? even?))
        r3s (collect-with foo-numbers-mixed (all? number? even?) :plevel 1)
        r4 (collect-with (into [] foo-numbers-mixed) (all? number? even?))]

    (are [x y] (= x y)
      13 (count r1)
      13 (count r2)
      5 (count r3)
      (sort r3) (sort r3s)
      5 (count r4)
      ) ) )
