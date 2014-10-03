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
  (let [r1 (pivot foo-numbers "" :b [number?] :p divisible-by? :v [2 3 4])
        r2 (pivot foo-numbers "foo" :b [number?] :p divisible-by?  :v [2 3 4])
        r3 (pivot foo-numbers "foo" :b [number?] :p divisible-by? :v [2 3 4] :plevel 2)
        ;r1 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
        ]

    (are [x y] (= x y)
      3 (-> "pivot-4" r1)
      6 (-> "pivot-3" r1)
      5 (-> "pivot-2" r1)
      ;
      3 (-> "foo_pivot-4" r2)
      6 (-> "foo_pivot-3" r2)
      5 (-> "foo_pivot-2" r2)
      ;
      3 (-> "foo_pivot-4" r3)
      6 (-> "foo_pivot-3" r3)
      5 (-> "foo_pivot-2" r3)
      ) ) )

(def foo-numbers-mixed '(2 3 4 5 9 "a" 11 12 15 20 21 "b" 25 26 27))

;(deftest test-build-pivot-functions-for-matrix
;  (let [r1 (build-pivot-groups-matrix [divisible-by?] (list '(2 3 4)) "pivot")
;        r2 (build-pivot-groups-matrix [divisible-by? divisible-by?] (list '(2 3) '(4 5)) "2x2")
;        r3 (build-pivot-groups-matrix [divisible-by? divisible-by? divisible-by?] (list '(2 3) '(4 5 6) '(7 8)) "2x3x2")
;        ]
;
;    (are [x y] (= x y)
;      1 (count r1)
;      3 (count (first r1))
;      2 (:pivot (meta (first (first r1))))
;      4 (:pivot (meta (last (first r1))))
;      ;
;      2 (count r2)
;      2 (count (nth r2 0))
;      2 (count (nth r2 1))
;      2 (:pivot (meta (first (first r2))))
;      3 (:pivot (meta (last (first r2))))
;      4 (:pivot (meta (first (last r2))))
;      5 (:pivot (meta (last (last r2))))
;      ) ) )

;(deftest test-build-matrix
;  (let [base [number?]
;        l1 (list '(2 3 4))
;        l2 (list '(2 3) '(4 5))
;        l3 (list '(2 3) '(4 5 6) '(7 8))
;        pg1 (build-pivot-groups-matrix [divisible-by?] l1 "pg1")
;        pg2 (build-pivot-groups-matrix [divisible-by? divisible-by?] l2 "pg2")
;        pg3 (build-pivot-groups-matrix [divisible-by? divisible-by? divisible-by?] l3 "pg3")
;        r1 (build-matrix c/all? base pg1)
;        r2 (build-matrix c/all? base pg2)
;        r3 (build-matrix c/all? base pg3)
;        ]
;
;    (are [x y] (= x y)
;      3 (count r1)
;      "pg1-pivots_[2]" (:name (meta (nth r1 0)))
;      "pg1-pivots_[4]" (:name (meta (nth r1 2)))
;      4 (count r2)
;      "pg2-pivots_[2|4]" (:name (meta (nth r2 0)))
;      "pg2-pivots_[3|5]" (:name (meta (nth r2 3)))
;      12 (count r3)
;      "pg3-pivots_[2|4|7]" (:name (meta (nth r3 0)))
;      "pg3-pivots_[3|6|8]" (:name (meta (nth r3 11)))
;      ) ) )

(def hundred (range 1 100))
; reducers/fold requires [] for multi-threads
(def sc (into [] (range 1 1001)))
; Usually takes <= 0.8 seconds for a million data points on 6 core AMD (3.4 ghz)

(deftest test-pivot-matrix
  (let [even-numbers [number? even?]
        divyX2 [divisible-by? divisible-by?]
        r1 (pivot-matrix hundred "r1"  :b [number? even?] :p [divisible-by?] :v [(range 2 5)])
        r2 (pivot-matrix hundred "r2" :b even-numbers :p [divisible-by? divisible-by?] :v [(range 2 5) (range 6 8)])
        r3p (pivot-matrix hundred "r1" :b even-numbers :p [divisible-by?] :v (list (range 2 5)) :plevel 2)
        r4p (pivot-matrix hundred "r2" :b even-numbers :p divyX2 :v [(range 2 5) (range 6 8)] :plevel 2)
        r5pp (pivot-matrix hundred "r1" :b even-numbers :p [divisible-by?] :v (list (range 2 5)) :plevel 3)
        r6pp (pivot-matrix hundred "r2" :b even-numbers :p divyX2 :v [(range 2 5) (range 6 8)] :plevel 3)

        ; performance testing
        ;lc (into [] (range 1 1000001))
        ;r7p (t/perf (pivot-matrix lc "r2lc" :b even-numbers :p divyX2 :v [(range 2 11) (range 7 18)] :plevel 3) "")
        ]

    (are [x y] (= x y)
      3 (count r1)
      49 (-> "r1-pivots_[2]" r1)
      16 (-> "r1-pivots_[3]" r1)
      24 (-> "r1-pivots_[4]" r1)
      ;
      6 (count r2)
      16 (-> "r2-pivots_[3|6]" r2)
      7 (-> "r2-pivots_[2|7]" r2)
      2 (-> "r2-pivots_[3|7]" r2)
      ;
      r3p r1
      r4p r2
      r5pp r1
      r6pp r2
      ) ) )