;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:author "dmillett"} clash.pivot_test
  (:require [clojure.math.combinatorics :as cmb]
            [clash.core :as c]
            [clash.tools :as t]
            [clojure.string :as s])
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
      3 (:result (get r1 "pivot_[4]"))
      6 (:result (get r1 "pivot_[3]"))
      5 (:result (get r1 "pivot_[2]"))
      ;
      3 (:result (get r2 "foo_[4]"))
      6 (:result (get r2 "foo_[3]"))
      5 (:result (get r2 "foo_[2]"))
      ;
      3 (:result (get r3 "foo_[4]"))
      6 (:result (get r3 "foo_[3]"))
      5 (:result (get r3 "foo_[2]"))
      ;
      4 (:result (get r4 "foo2_[[2 3]]"))
      2 (:result (get r4 "foo2_[[4 5]]"))
      )
    ) )

(def foo-numbers-mixed '(2 3 4 5 9 "a" 11 12 15 20 21 "b" 25 26 27))

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
        ;
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
        ;r10 (t/perf (pivot-matrix lc "r2lc" :b even-numbers :p divyX2 :v [(range 2 11) (range 7 18)] :plevel 3) "")
        ]
    (are [x y] (= x y)
      3 (count r1)
      49 (get-in r1 ["r1_[2]" :count])
      16 (get-in r1 ["r1_[3]" :count])
      24 (get-in r1 ["r1_[4]" :count])
      ;
      6 (count r2)
      16 (get-in r2 ["r2_[3|6]" :count])
      7 (get-in r2 ["r2_[2|7]" :count])
      2 (get-in r2 ["r2_[3|7]" :count])
      ;
      6 (count r7)
      16 (get-in r7 ["r7_[3|[2 4]]" :count])
      8 (get-in r7 ["r7_[4|[2 4]]" :count])
      2 (get-in r7 ["r7_[4|[4 5]]" :count])
      ;
      3 (count r3p)
      49 (get-in r3p ["r1_[2]" :count])
      16 (get-in r3p ["r1_[3]" :count])
      24 (get-in r3p ["r1_[4]" :count])
      ;
      6 (count r4p)
      16 (get-in r4p ["r2_[3|6]" :count])
      7 (get-in r4p ["r2_[2|7]" :count])
      2 (get-in r4p ["r2_[3|7]" :count])
      ;
      3 (count r5pp)
      49 (get-in r5pp ["r1_[2]" :count])
      16 (get-in r5pp ["r1_[3]" :count])
      24 (get-in r5pp ["r1_[4]" :count])
      ;
      6 (count r6pp)
      16 (get-in r6pp ["r2_[3|6]" :count])
      7 (get-in r6pp ["r2_[2|7]" :count])
      2 (get-in r6pp ["r2_[3|7]" :count])
      ;
      18 (count r8)
      12 (get-in r8 ["r8_[2|[3 5]]" :count])
      ;r1 r1e
      (keys r1) (keys r1e)
      '(49 24 16) (map #(:count %) (vals r1))
      '(49 24 16) (map #(:count %) (vals r1e))
      (keys r2) (keys r2e)
      '(16 16 8 7 3 2) (map #(:count %) (vals r2))
      '(16 16 8 7 3 2) (map #(:count %) (vals r2e))
      ;r3p r3pe
      (keys r3p) (keys r3pe)
      '(49 24 16) (map #(:count %) (vals r3p))
      '(49 24 16) (map #(:count %) (vals r3pe))
      ;r7 r7e anonymous functions are equivalent
      (keys r7e) (keys r7)
      (map #(:count %) (vals r7e)) (map #(:count %) (vals r7))
      ) ) )

(deftest test-pivot-matrix*
  (let [data ["foo" "bar" "zoo"]
        fx? (fn [c] #(s/includes? % c))
        r1 (pivot-matrix* data "r1" :pivots [{:f fx? :v ["a" "b" "c"]} {:f fx? :v ["r" "d"]}])
        r2 (pivot-matrix* data "r2" :combfx? t/none? :pivots [{:f fx? :v ["a" "b" "c"]} {:f fx? :v ["r" "d"]}])
        r3 (pivot-matrix* data "r3" :combfx? t/any? :pivots [{:f fx? :v ["a" "b" "c"]} {:f fx? :v ["r" "d"]}])
        ]
    (are [x y] (= x y)
      1 (get-in r1 ["r1_[b|r]" :count])
      0 (get-in r1 ["r1_[c|r]" :count])
      ;
      3 (get-in r2 ["r2_[c|d]" :count])
      2 (get-in r2 ["r2_[c|r]" :count])
      2 (get-in r2 ["r2_[b|r]" :count])
      ;
      1 (get-in r3 ["r3_[c|r]" :count])
      1 (get-in r3 ["r3_[b|r]" :count])
      0 (get-in r3 ["r3_[c|d]" :count])
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

(deftest test-pivot-compare
  (let [c1 (range 0 20)
        c2 (range 10 30)
        expected {"divy1_[3]" true, "divy1_[4]" false, "divy1_[2]" false}]
    (are [x y] (= x y)
      {} (pivot-compare c1 c2 "empty" >)
      expected (pivot-compare c1 c2 "divy1" > :b [number?] :p divisible-by? :v [2 3 4])
      expected (pivot-compare c1 c2 "divy1" > :b [number?] :p divisible-by? :v [2 3 4] :plevel 2)
      ) ) )


(deftest test-pivot-matrix-compare
  (let [r1 (pivot-matrix-compare (range 1 50) (range 50 120) "foo" ratio :b [number?]
                                                                         :p [divisible-by?]
                                                                         :v [(range 2 6)])]
    (are [x y] (= x y)
      4 (count r1)
      0.706 (get-in r1 ["foo_[4]" :result])
      0.696 (get-in r1 ["foo_[3]" :result])
      0.686 (get-in r1 ["foo_[2]" :result])
      0.643 (get-in r1 ["foo_[5]" :result])
      )
    ) )

(deftest test-pivot-rs
  (let [hundred (range 1 100)
        m1 (pivot-matrix hundred "foo" :b [even?] :p [divisible-by?] :v [(range 2 6)])
        r1 (pivot-rs hundred m1 "foo_[5]")
        m2 (pivot-matrix-compare (range 1 50) (range 50 120) "foo" ratio :b [number?]
                                                                         :p [divisible-by?]
                                                                         :v [(range 2 6)])
        r2 (pivot-rs hundred m2 "foo_[5]")
        ]
    (are [x y] (= x y)
      9 (count r1)
      r1 [10 20 30 40 50 60 70 80 90]
      19 (count r2)
      r2 [5 10 15 20 25 30 35 40 45 50 55 60 65 70 75 80 85 90 95]
      )
  ) )

(deftest test-filter-pivots
  (let [pm1 (pivot-matrix* hundred "find" :pivots [{:f divisible-by? :v (range 2 6)}])]
    (are [x y] (= x y)
      pm1 (filter-pivots pm1)
      24 (get-in (filter-pivots pm1 :cfx even?) ["find_[4]" :count])
      24 (get-in (filter-pivots pm1 :kterms ["4"]) ["find_[4]" :count])
      33 (get-in (filter-pivots pm1 :kterms ["3"] :cfx odd?) ["find_[3]" :count])
      ) ) )

(deftest test-haystack
  (let [d1 [{:a {:c "c"} :b {:d "d"}} {:a {:c "c1"} :b {:d "d"}} {:a {:c "c"} :b {:d "d1"}} {:a {:c "c1"} :b {:d "d"}}]
        d2 [{:a {:b 1 :c 2 :d 3}} {:a {:b 1 :c 3 :d 3}} {:a {:b 2 :c 3 :d 3}}]
        h1 (haystack d1 :vfkpath [:a] :vffx #(t/sort-value-frequencies %))
        h2 (haystack d2 :vfkpath [:a])
        h3 (haystack d2 :vfkpath [:a] :vfkset [:b :c])
        h4 (haystack d2 :vfkpath [:a] :vfkset [:b :c] :vfkvfx (fn [[_ v]] (even? v)))
        h5 (haystack d2 :vfkpath [:a] :vfkset [:b :c] :vfkvfx (fn [[k _]] (even? k)))
        ]

    (are [x y] (= x y)
      2 (count h1)
      2 (:count (get h1 "haystack([:a :c])_[c]"))
      2 (:count (get h1 "haystack([:a :c])_[c1]"))
      ;
      4 (count h2)
      1 (:count (get h2 "haystack([:a :b]|[:a :c]|[:a :d])_[2|3|3]"))
      1 (:count (get h2 "haystack([:a :b]|[:a :c]|[:a :d])_[1|3|3]"))
      1 (:count (get h2 "haystack([:a :b]|[:a :c]|[:a :d])_[1|2|3]"))
      0 (:count (get h2 "haystack([:a :b]|[:a :c]|[:a :d])_[2|2|3]"))
      ;
      4 (count h3)
      1 (:count (get h3 "haystack([:a :b]|[:a :c])_[2|3]"))
      1 (:count (get h3 "haystack([:a :b]|[:a :c])_[1|3]"))
      1 (:count (get h3 "haystack([:a :b]|[:a :c])_[1|2]"))
      0 (:count (get h3 "haystack([:a :b]|[:a :c])_[2|2]"))
      ;
      ; value frequencies: '{:b {1 2, 2 1}, :c {2 1, 3 2}}'
      1 (count h4)
      1 (:count (get h4 "haystack([:a :b]|[:a :c])_[1|3]"))
      ;;
      1 (count h5)
      0 (:count (get h5 "haystack([:a :b]|[:a :c])_[2|2]"))
      )))