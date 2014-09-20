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
    0 (count-with medium_complexity (is-zoo? "PIG"))
    1 (count-with medium_complexity (is-zoo? "ZOO"))
    1 (count-with medium_complexity is-fur-odd?)
    0 (count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "BAR")))
    1 (count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "ZAP")))
    ) )

(deftest test-with-all-predicates
  (let [result1 (all-preds? 5 even?)
        result2 (all-preds? 4 even?)
        result3 (all-preds? 4 number? even?)
        result4 (all-preds? 4 number? odd?)
        result5 (all-preds? 12 number? even? #(= 0 (mod % 6)))]
    (is (not result1))
    (is result2)
    (is result3)
    (is (not result4))
    (is result5)
    ) )

(deftest test-with-any-predicates
  (let [result1 (any-preds? 5 even?)
        result2 (any-preds? 4 even?)
        result3 (any-preds? 4 number? even?)
        result4 (any-preds? 4 number? odd?)
        result5 (any-preds? 12 number? even? #(= 0 (mod % 5)))]
    (is (not result1))
    (is result2)
    (is result3)
    (is result4)
    (is result5)
    ) )

(defn- divisible-by?
  [x]
  (fn [n] (= 0 (mod n x))) )

(deftest any-and-all?
  (let [result1 ((all? number? even?) 10)
        result2 ((all? number? odd?) 10)
        result3 ((any? number? even?) 11)
        result4 ((all? number? even? (divisible-by? 5)) 10)
        result5 ((any? number? odd? even?) 16)
        result5 ((all? number? (any? (divisible-by? 6) (divisible-by? 4))) 16)]

    (is result1)
    (is (not result2))
    (is result3)
    (is result4)
    (is result5)
    ) )

(deftest test-until
  (let [r1 (until? even? '("foo" "bar"))
        r2 (until? number? '(1 2 3))
        r3 (until? number? '("foo" 2 "bar"))
        r4 (until? even? '("foo" 1 "bar" 3 4 "zoo"))
        r5 (until? even? '("foo" 1 "bar" 3 5))]

    (is (not r1))
    (is r2)
    (is r3)
    (is r4)
    (is (not r5))
    ) )

(deftest test-take-until
  (let [r1 (take-until number? '(1 2 3))
        r2 (take-until number? '("foo" "bar" 3 4))
        r3 (take-until even? ["foo" "bar" 3 5 6 "zoo"])
        r4 (take-until even? '("foo" "bar"))]

    (are [x y] (= x y)

      '(1) r1
      '(3 "bar" "foo") r2
      '(6 5 3 "bar" "foo") r3
      '() r4
      ) ) )

(def foo-numbers '(2 3 4 5 9 11 12 15 20 21 25 26 27))
(def foo-numbers-mixed '(2 3 4 5 9 "a" 11 12 15 20 21 "b" 25 26 27))

(deftest test-count-with
  (let [r1 (count-with foo-numbers-mixed (all? number?))
        r2 (count-with foo-numbers-mixed (all? number? even?))
        r3 (count-with foo-numbers-mixed (all? number? even?) 37)]

    (are [x y] (= x y)
      13 r1
      5  r2
      42 r3
      ) ) )

(deftest test-pcount-with
  (let [r1 (pcount-with foo-numbers-mixed (all? number?))
        r2 (pcount-with (into [] foo-numbers-mixed) (all? number?))
        r3 (pcount-with foo-numbers-mixed (all? number? even?))
        r4 (pcount-with (into [] foo-numbers-mixed) (all? number? even?))
        r5 (pcount-with (into [] foo-numbers-mixed) (all? number? even?) 37)]

    (are [x y] (= x y)
      13 r1
      13 r2
      5 r3
      5 r4
      42 r5
      ) ) )

(deftest test-collect-with
  (let [r1 (collect-with foo-numbers-mixed (all? number?))
        r2 (collect-with foo-numbers-mixed (all? number? even?))]

    (are [x y] (= x y)
      13 (count r1)
      5 (count r2)
      ) ) )

(deftest test-pcollect-with
  (let [r1 (pcollect-with foo-numbers-mixed (all? number?))
        r2 (pcollect-with (into [] foo-numbers-mixed) (all? number?))
        r3 (pcollect-with foo-numbers-mixed (all? number? even?))
        r4 (pcollect-with (into [] foo-numbers-mixed) (all? number? even?))]

    (are [x y] (= x y)
      13 (count r1)
      13 (count (concat r2))
      5 (count r3)
      5 (count r4)
      ) ) )
