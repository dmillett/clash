;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:author dmillett} clash.pivot_test
  (:require [clojure.math.combinatorics :as cmb]
            [clash.core :as c]
            [clash.tools :as t])
  (:use [clojure.test]
        [clash.pivot]))

(defn- divisible-by?
  [x]
  (fn [n] (zero? (mod n x)) ) )

(defn- foo-divide?
  "2 arg nonsensical division function."
  [d i]
  (fn [n] (zero? (mod n (+ d i)))))

(def foo-numbers '(2 3 4 5 9 11 12 15 20 21 25 26 27))

(deftest test-pivot
  (let [r1 (pivot foo-numbers "" :b [number?] :p divisible-by? :v [2 3 4])
        r2 (pivot foo-numbers "foo" :b [number?] :p divisible-by?  :v [2 3 4])
        r3 (pivot foo-numbers "foo" :b [number?] :p divisible-by? :v [2 3 4] :plevel 2)
        r4 (pivot foo-numbers "foo2" :b [number?] :p foo-divide? :v [[2 3] [4 5]])
        ;r5 (perf (pivot foo-numbers [number?] divide-by-x? '(2 3 4)) "(pivot a)")
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
      ;
      4 (-> "foo2_pivot-[2 3]" r4)
      2 (-> "foo2_pivot-[4 5]" r4)
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
        cp (map #(into [] %) (cmb/cartesian-product (range 2 5) (range 5 8)))
        r1 (pivot-matrix hundred "r1"  :b [number? even?] :p [divisible-by?] :v [(range 2 5)])
        r2 (pivot-matrix hundred "r2" :b even-numbers :p [divisible-by? divisible-by?] :v [(range 2 5) (range 6 8)])
        r3p (pivot-matrix hundred "r1" :b even-numbers :p [divisible-by?] :v (list (range 2 5)) :plevel 2)
        r4p (pivot-matrix hundred "r2" :b even-numbers :p divyX2 :v [(range 2 5) (range 6 8)] :plevel 2)
        r5pp (pivot-matrix hundred "r1" :b even-numbers :p [divisible-by?] :v (list (range 2 5)) :plevel 3)
        r6pp (pivot-matrix hundred "r2" :b even-numbers :p divyX2 :v [(range 2 5) (range 6 8)] :plevel 3)
        r7 (pivot-matrix hundred "r7" :b even-numbers :p [divisible-by? foo-divide?] :v [(range 2 5) '([2 4] [4 5])])
        r8 (pivot-matrix hundred "r8" :b even-numbers :p [divisible-by? foo-divide?] :v [(range 2 4) cp])

        r1e (pivot-matrix-e hundred "r1"  :base [number? even?] :pivot [{:f divisible-by? :v (range 2 5)}])
        r2e (pivot-matrix-e hundred "r2" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)} {:f divisible-by? :v (range 6 8)}])
        r3pe (pivot-matrix-e hundred "r1" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)}] :plevel 2)
        r4pe (pivot-matrix-e hundred "r2" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)} {:f divisible-by? :v (range 6 8)}] :plevel 2)
        r5ppe (pivot-matrix-e hundred "r1" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)}] :plevel 3)
        r6ppe (pivot-matrix-e hundred "r2" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)} {:f divisible-by? :v (range 6 8)}] :plevel 3)
        r7e (pivot-matrix-e hundred "r7" :base even-numbers :pivot [{:f divisible-by? :v (range 2 5)} {:f foo-divide? :v '([2 4] [4 5])}])
        r8e (pivot-matrix-e hundred "r8" :base even-numbers :pivot [{:f divisible-by? :v (range 2 4)} {:f foo-divide? :v cp}])

        ; performance testing
        ;lc (into [] (range 1 1000001))
        ;r9 (t/perf (pivot-matrix lc "r2lc" :b even-numbers :p divyX2 :v [(range 2 11) (range 7 18)] :plevel 3) "")
        ]

    (are [x y] (= x y)
      3 (count r1)
      49 (get-in r1 ["r1-pivots_[2]" :count])
      16 (get-in r1 ["r1-pivots_[3]" :count])
      24 (get-in r1 ["r1-pivots_[4]" :count])
      ;
      6 (count r2)
      16 (get-in r2 ["r2-pivots_[3|6]" :count])
      7 (get-in r2 ["r2-pivots_[2|7]" :count])
      2 (get-in r2 ["r2-pivots_[3|7]" :count])
      ;
      6 (count r7)
      16 (get-in r7 ["r7-pivots_[3|[2 4]]" :count])
      8 (get-in r7 ["r7-pivots_[4|[2 4]]" :count])
      2 (get-in r7 ["r7-pivots_[4|[4 5]]" :count])
      ;
      3 (count r3p)
      49 (get-in r3p ["r1-pivots_[2]" :count])
      16 (get-in r3p ["r1-pivots_[3]" :count])
      24 (get-in r3p ["r1-pivots_[4]" :count])
      ;
      6 (count r4p)
      16 (get-in r4p ["r2-pivots_[3|6]" :count])
      7 (get-in r4p ["r2-pivots_[2|7]" :count])
      2 (get-in r4p ["r2-pivots_[3|7]" :count])
      ;
      3 (count r5pp)
      49 (get-in r5pp ["r1-pivots_[2]" :count])
      16 (get-in r5pp ["r1-pivots_[3]" :count])
      24 (get-in r5pp ["r1-pivots_[4]" :count])
      ;
      6 (count r6pp)
      16 (get-in r6pp ["r2-pivots_[3|6]" :count])
      7 (get-in r6pp ["r2-pivots_[2|7]" :count])
      2 (get-in r6pp ["r2-pivots_[3|7]" :count])
      ;
      18 (count r8)
      12 (get-in r8 ["r8-pivots_[2|[3 5]]" :count])
      ;
      r1 r1e
      r2 r2e
      r3p r3pe
      ) ) )

(defn- ratio
  "The ratio of (/ a b) for counts of predicate matches."
  [a b]
  (cond
    (and (nil? a) (nil? b)) nil
    (nil? a) 0
    (nil? b) (/ a 1)
    :else (read-string (format "%.3f" (/ a (float b))))
    ) )

(deftest test-pivot-matrix-compare
  (let [r1 (pivot-matrix-compare (range 1 50) (range 50 120) "foo" ratio :b [number?]
                                                                         :p [divisible-by?]
                                                                         :v [(range 2 6)])
        ]
    (are [x y] (= x y)
      4 (count r1)
      0.706 (get-in r1 ["foo-pivots_[4]" :result])
      0.696 (get-in r1 ["foo-pivots_[3]" :result])
      0.686 (get-in r1 ["foo-pivots_[2]" :result])
      0.643 (get-in r1 ["foo-pivots_[5]" :result])
      ) ) )

(deftest test-get-rs-from-matrix
  (let [hundred (range 1 100)
        m1 (pivot-matrix hundred "foo" :b [even?] :p [divisible-by?] :v [(range 2 6)])
        r1 (get-rs-from-matrix hundred m1 "foo-pivots_[5]")
        m2 (pivot-matrix-compare (range 1 50) (range 50 120) "foo" ratio :b [number?]
                                                                         :p [divisible-by?]
                                                                         :v [(range 2 6)])
        r2 (get-rs-from-matrix hundred m2 "foo-pivots_[5]")
        ]

    (are [x y] (= x y)
      9 (count r1)
      r1 '(90 80 70 60 50 40 30 20 10)
      19 (count r2)
      r2 '(95 90 85 80 75 70 65 60 55 50 45 40 35 30 25 20 15 10 5)
      ) ) )