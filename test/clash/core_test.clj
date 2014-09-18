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

(deftest test-pivot
  (let [r1 (pivot foo-numbers [number?] divisible-by? '(2 3 4))
        r2 (pivot foo-numbers [number?] divisible-by? '(2 3 4) "is-number")
        ;r1 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
        ]

    ;(println "r1" r1)

    (are [x y] (= x y)
      3 (-> "pivot-4" r1)
      6 (-> "pivot-3" r1)
      5 (-> "pivot-2" r1)
      ;
      3 (-> "is-number_pivot-4" r2)
      6 (-> "is-number_pivot-3" r2)
      5 (-> "is-number_pivot-2" r2)
      ) ) )

(deftest test-ppivot
  (let [r1 (ppivot foo-numbers [number?] divisible-by? '(2 3 4))
        r2 (ppivot foo-numbers [number?] divisible-by? '(2 3 4) "is-number")
        ;r1 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
        ]

    ;(println "r1" r1)

    (are [x y] (= x y)
      3 (-> "pivot-4" r1)
      6 (-> "pivot-3" r1)
      5 (-> "pivot-2" r1)
      ;
      3 (-> "is-number_pivot-4" r2)
      6 (-> "is-number_pivot-3" r2)
      5 (-> "is-number_pivot-2" r2)
      ) ) )

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


(deftest test-build-pivot-functions
  (let [r1 (build-pivot-functions [divisible-by?] (list '(2 3 4)) "pivot")
        r2 (build-pivot-functions [divisible-by? divisible-by?] (list '(2 3) '(4 5)) "pivot")]

    (are [x y] (= x y)
      3 (count r1)
      "pivot-2" (:name (meta (first r1)))
      "pivot-4" (:name (meta (last r1)))
      4 (count r2)
      "pivot-2" (:name (meta (first r2)))
      "pivot-5" (:name (meta (last r2)))
      ) ) )

(deftest test-build-pivot-functions2
  (let [r1 (build-pivot-functions2 [divisible-by?] (list '(2 3 4)) "pivot")
        r2 (build-pivot-functions2 [divisible-by? divisible-by?] (list '(2 3) '(4 5)) "pivot")]

    (are [x y] (= x y)
      1 (count r1)
      3 (count (first r1))
      "pivot-2" (:name (meta (first (first r1))))
      "pivot-4" (:name (meta (last (first r1))))
      2 (count r2)
      2 (count (first r2))
      2 (count (last r2))
      "pivot-4" (:name (meta (first (first r2))))
      "pivot-5" (:name (meta (last (first r2))))
      "pivot-2" (:name (meta (first (last r2))))
      "pivot-3" (:name (meta (last (last r2))))
      ) ) )

(defn- merge-meta
  "Merge metadata ':name' from two objects into a string
  separated by a delimiter"
  [a b delim]
  (str (:name (meta a)) delim (:name (meta b))) )

(defn- prepend-meta
  "Prepend text + delim to the metadata for :name of the current
  object 'b'"
  [txt b delim]
  (let [mta (:name (meta b))]
    ;(println "pm_mta:" mta)
    (if txt
      (str txt delim mta)
      mta
      ) ) )

;; How to combine meta information for multiple functions
(defn- conj-meta
  "Combine the meta for each item in a collection during (conj)
  so the resulting collection has cumulative meta :name, separated
  by '|'"
  ([col v] (with-meta (conj col v) {:name (merge-meta col v "|")}) )
  ([col v & values]
    ;(println "cm: " (meta v) "," (meta values))
    (loop [c col
           nxt v
           rst values
           mtext (:name (meta c))]

      (if-not rst
        (with-meta (conj c nxt) {:name (prepend-meta mtext nxt "|")})
        (recur (conj c nxt) (first rst) (next rst) (prepend-meta mtext nxt "|"))
        ) )
    ) )

(deftest test-conj-meta
  (let [a (with-meta '() {:name "empty"})
        b (with-meta '("foo") {:name "foo"})
        c (with-meta '("bar") {:name "bar"})
        r1 (conj-meta a b)
        r2 (conj-meta a b c)
        r3 (conj-meta '() b c)]

    (are [x y] (= x y)
      "empty|foo" (:name (meta r1))
      "empty|foo|bar" (:name (meta r2))
      "foo|bar" (:name (meta r3))
      ) ) )

(defn- combine-functions
  "Carry the metadata :name forward from the pivot functions"
  [f preds pivots]
  ; copy meta data from pivot functions when appending them to predicates
  (let [mt (:name (meta pivots))]
    (map #(with-meta
            (apply f (conj preds %))
            {:name mt} )
    pivots) ) )

(defn build-matrix
  "Build a single list of predicate groups that comprise a flattened
  matrix for each collection of pivots within pivot_groups. This supports
  up to 4 different predicate groups. Perhaps this should be a macro?"
  [f base pgs]
  (let [cnt (count pgs)]
    (cond
      (= 1 cnt) (combine-functions f base (first pgs))
      (= 2 cnt) (for [a (nth pgs 0) b (nth pgs 1)]
                  (combine-functions f base (conj-meta '() a b) ))
      (= 3 cnt) (for [a (nth pgs 0) b (nth pgs 1) c (nth pgs 2)]
                  (combine-functions f base (conj-meta '() a b c)) )
      (= 4 cnt) (for [a (nth pgs 0) b (nth pgs 1) c (nth pgs 2) d (nth pgs 3)]
                  (combine-functions f base (conj-meta '() a b c d)) )
      (= 5 cnt) (for [a (nth pgs 0) b (nth pgs 1) c (nth pgs 2) d (nth pgs 3) e (nth pgs 4)]
                  (combine-functions f base (conj-meta '() a b c d e)) )
      ) ) )

(deftest test-build-matrix
  (let [base [number?]
        l1 (list '(2 3 4))
        l2 (list '(2 3) '(4 5))
        l3 (list '(2 3) '(4 5 6) '(7 8))
        pg1 (build-pivot-functions2 [divisible-by?] l1 "pivot")
        pg2 (build-pivot-functions2 [divisible-by? divisible-by?] l2 "pivot")
        pg3 (build-pivot-functions2 [divisible-by? divisible-by? divisible-by?] l3 "pivot")
        ;r1 (build-matrix all? base pg1)
        r2 (apply concat (build-matrix all? base pg2))
        ;r3 (apply concat (build-matrix all? base pg3))
        ]

    ;(println r2)
    ;(println (meta (nth r2 0)))

    (are [x y] (= x y)
      1 1
      ;3 (count r1)
      ;8 (count r2)
      ;36 (count r3)
      ) ) )