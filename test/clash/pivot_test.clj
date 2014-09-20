;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:author dmillett} clash.pivot_test
  (:require [clash.core :as c]
            [clash.tools :as t])
  (:use [clojure.test]
        [clash.pivot]))

(defn- divisible-by?
  [x]
  (fn [n] (if (number? n) (= 0 (mod n x)) false) ) )

(def foo-numbers '(2 3 4 5 9 11 12 15 20 21 25 26 27))

(deftest test-pivot
  (let [r1 (pivot foo-numbers [number?] divisible-by? '(2 3 4))
        r2 (pivot foo-numbers [number?] divisible-by? '(2 3 4) "is-number")
        ;r1 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
        ]

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
  (let [r1 (p-pivot foo-numbers [number?] divisible-by? '(2 3 4))
        r2 (p-pivot foo-numbers [number?] divisible-by? '(2 3 4) "is-number")
        ;r1 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
        ]

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

(deftest test-build-pivot-functions-for-matrix
  (let [r1 (build-pivot-groups-matrix [divisible-by?] (list '(2 3 4)) "pivot")
        r2 (build-pivot-groups-matrix [divisible-by? divisible-by?] (list '(2 3) '(4 5)) "2x2")
        r3 (build-pivot-groups-matrix [divisible-by? divisible-by? divisible-by?] (list '(2 3) '(4 5 6) '(7 8)) "2x3x2")
        ]

    (are [x y] (= x y)
      1 (count r1)
      3 (count (first r1))
      2 (:pivot (meta (first (first r1))))
      4 (:pivot (meta (last (first r1))))
      ;
      2 (count r2)
      2 (count (nth r2 0))
      2 (count (nth r2 1))
      2 (:pivot (meta (first (first r2))))
      3 (:pivot (meta (last (first r2))))
      4 (:pivot (meta (first (last r2))))
      5 (:pivot (meta (last (last r2))))
      ) ) )

(deftest test-conj-meta-matrix
  (let [a (with-meta '() {})
        b (with-meta '("foo") {:base_msg "foo" :pivot "f"})
        c (with-meta '("bar") {:base_msg "foo" :pivot "b"})
        d (with-meta divisible-by? {:base_msg "foo" :pivot "A"})
        e (with-meta divisible-by? {:base_msg "foo" :pivot "B"})
        r1 (conj-meta-matrix a b)
        r2 (conj-meta-matrix a b c)
        r3 (conj-meta-matrix '() b c)
        r4 (conj-meta-matrix [] d e)]

    (are [x y] (= x y)
      "foo-pivot_f" (:name (meta r1))
      "foo-pivot_f|b" (:name (meta r2))
      "foo-pivot_f|b" (:name (meta r3))
      "foo-pivot_A|B" (:name (meta r4))
      ) ) )

(deftest test-build-matrix
  (let [base [number?]
        l1 (list '(2 3 4))
        l2 (list '(2 3) '(4 5))
        l3 (list '(2 3) '(4 5 6) '(7 8))
        pg1 (build-pivot-groups-matrix [divisible-by?] l1 "pg1")
        pg2 (build-pivot-groups-matrix [divisible-by? divisible-by?] l2 "pg2")
        pg3 (build-pivot-groups-matrix [divisible-by? divisible-by? divisible-by?] l3 "pg3")
        r1 (build-matrix c/all? base pg1)
        r2 (build-matrix c/all? base pg2)
        r3 (build-matrix c/all? base pg3)]

    (are [x y] (= x y)
      1 1
      3 (count r1)
      4 (count r2)
      12 (count r3)
      ) ) )

(def hundred (range 1 100))

(deftest test-pivot-matrix
  (let [r1 (pivot-matrix hundred [number? even?] [divisible-by?] (list (range 2 5)) "r1")
        r2 (pivot-matrix hundred [number? even?] [divisible-by? divisible-by?] [(range 2 5) (range 6 8)] "r2")]

    (are [x y] (= x y)
      3 (count r1)
      49 (-> "r1-pivot_2" r1)
      16 (-> "r1-pivot_3" r1)
      24 (-> "r1-pivot_4" r1)
      ;
      6 (count r2)
      16 (-> "r2-pivot_3|6" r2)
      7 (-> "r2-pivot_2|7" r2)
      2 (-> "r2-pivot_3|7" r2)
      ) ) )

(deftest test-pivot-matrix-x
  (let [r1 (pivot-matrix-x hundred [number? even?] "r1" :pivots [divisible-by?] :values (list (range 2 5)))
        r2 (pivot-matrix-x hundred [number? even?] "r2" :pivots [divisible-by? divisible-by?] :values [(range 2 5) (range 6 8)])
        ]

    (are [x y] (= x y)
      3 (count r1)
      49 (-> "r1-pivot_2" r1)
      16 (-> "r1-pivot_3" r1)
      24 (-> "r1-pivot_4" r1)
      ;
      6 (count r2)
      16 (-> "r2-pivot_3|6" r2)
      7 (-> "r2-pivot_2|7" r2)
      2 (-> "r2-pivot_3|7" r2)
      ) ) )


; reducers/fold requires [] for multi-threads
(def sc (into [] (range 1 1001)))
; Usually takes <= 0.8 seconds for a million data points on 6 core AMD (3.4 ghz)
(def lc (into [] (range 1 1000001)))

(deftest test-ppivot-matrix
  (let [r1 (p-pivot-matrix hundred [number? even?] [divisible-by?] (list (range 2 5)) "r1")
        r2 (p-pivot-matrix hundred [number? even?] [divisible-by? divisible-by?] [(range 2 5) (range 6 8)] "r2")
;        r3 (t/perf (ppivot-matrix lc [number? even?] [divisible-by? divisible-by?] [(range 2 5) (range 6 8)] "r3") "")
;        r4 (t/perf (pp-pivot-matrix lc [number? even?] [divisible-by? divisible-by?] [(range 2 7) (range 7 12)] "r4") "")
        ]

    (are [x y] (= x y)
      3 (count r1)
      49 (-> "r1-pivot_2" r1)
      16 (-> "r1-pivot_3" r1)
      24 (-> "r1-pivot_4" r1)
      ;
      6 (count r2)
      16 (-> "r2-pivot_3|6" r2)
      7 (-> "r2-pivot_2|7" r2)
      2 (-> "r2-pivot_3|7" r2)
      ;
;      166666 (-> "r3-pivot_3|6" r3)
      ;
;      25 (count r4)
;      ["r4-pivot_4|8" 125000] (first r4)
      ) ) )

(deftest test-pp-pivot-matrix
  (let [r1 (pp-pivot-matrix hundred [number? even?] [divisible-by?] (list (range 2 5)) "r1")
        r2 (pp-pivot-matrix hundred [number? even?] [divisible-by? divisible-by?] [(range 2 5) (range 6 8)] "r2")
;        r3 (t/perf (pp-pivot-matrix lc [number? even?] [divisible-by? divisible-by?] [(range 2 5) (range 6 8)] "r3") "")
;        r4 (t/perf (pp-pivot-matrix lc [number? even?] [divisible-by? divisible-by?] [(range 2 7) (range 7 12)] "r4") "")
        ]

    (are [x y] (= x y)
      3 (count r1)
      49 (-> "r1-pivot_2" r1)
      16 (-> "r1-pivot_3" r1)
      24 (-> "r1-pivot_4" r1)
      ;
      6 (count r2)
      16 (-> "r2-pivot_3|6" r2)
      7 (-> "r2-pivot_2|7" r2)
      2 (-> "r2-pivot_3|7" r2)
      ;
;      166666 (-> "r3-pivot_3|6" r3)
      ;
;      25 (count r4)
;      ["r4-pivot_4|8" 125000] (first r4)
      ) ) )