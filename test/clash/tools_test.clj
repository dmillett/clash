;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.tools_test
  (:use [clash.tools]
        [clojure.test]))

(deftest test-formatf
  (is (= "2.00" (formatf 2 2)))
  (is (= "2.30" (formatf 2.3 2)))
  (is (= "1.5000" (formatf 1.5 4))) )

(deftest test-elapsed
  (is (= "t1 Time(ns):100" (elapsed 100 "t1" 0)))
  (is (= "t2 Time(ms):1.000" (elapsed 1000000 "t2" 4)))
  (is (= "t3 Time(s):1.000" (elapsed 1000000000 "t3" 4))) )

;; Test functions for perf and latency
(defn foobar [x] (* x x))
(defn phrase-it [result phrase] (str result phrase))

(deftest test-latency
  (let [result1 (latency (foobar 5))
        result2 (latency (phrase-it (foobar 10) ", the square of 10"))]

    (is (= 25 (-> result1 :result)))
    (is (not (nil? (-> result1 :latency_text))))
    (is (= "100, the square of 10" (-> result2 :result)))
    )
  )

(deftest test-sort-map-by-value
  (let [m1 {:a 1 :b 2 :c 3}
        m2 {:a 1 :b 2 :c 1 :d 3 :e 2}
        m3 {"a" 1 "b" 3 "c" 1}
        r1 (sort-map-by-value m1)
        r2 (sort-map-by-value m2)
        r3 (sort-map-by-value m3)]

    (are [x y] (= x y)
      3 (count r1)
      [:c 3] (first r1)
      [:a 1] (last r1)

      5 (count r2)
      [:d 3] (first r2)
      [:a 1] (last r2)

      3 (count r3)
      ["b" 3] (first r3)
      ["a" 1] (last r3)
      ) ) )

(deftest test-compare-map-with
  (let [m1 {:a 1 :b 2 :c 3 "d" 4}
        m2 {:a 2 :b 4 :c 5 "d" 7}
        f #(/ %1 (double %2))
        r1 (compare-map-with m1 m2 f)]

    ;(println r1)
    (are [x y] (= x y)
      0.5 (:a r1)
      0.5 (:b r1)
      0.6 (:c r1)
      ) ) )

;
; {top key { nested-key : frequency } }
; {:a {1 1, 3 1}, :b {2 1, 4 1}, :c {5 1}} ; :a-1 appears 1 time, :a-3 appears 1 time
(def mps1 [{:a 1 :b 2} {:a 3 :b 4 :c 5}])
(def mps2 [{:foo {:a "x" :b "y"}} {:foo {:a "xx" :b "yy" :c "zz"}}])

(deftest test-value-frequencies
  (let [r1 (value-frequencies {:a "a1" :b "b1"})
        r2 (value-frequencies {:a {"a1" 3}} {:a "a1" :b "b1"})
        r3 (value-frequencies {:a {"a1" 3}} {:a "a1" :b {:c "c1"}} :kpath [:b])
        r4 (value-frequencies {} {:a "a1" :b {:c "c1"}} :kpath [:b])
        r5 (value-frequencies {} {:a "a1" :b {:c "c1"}} :kpath [:f])
        ; ignores a kset of ':a' since it does not exist at depth [:b :c]
        r6 (value-frequencies {:a {"a1" 3}} {:a "a1" :b {:c {:d "d1" :e "e1"}}} :kpath [:b :c] :kset [:e :a])
        ]

    (are [x y] (= x y)
      1 (get-in r1 [:a "a1"])
      1 (get-in r1 [:b "b1"])
      1 (get-in r2 [:b "b1"])
      4 (get-in r2 [:a "a1"])
      2 (count r3)
      3 (get-in r3 [:a "a1"])
      1 (get-in r3 [:c "c1"])
      1 (count r4)
      1 (get-in r4 [:c "c1"])
      0 (count r5)
      2 (count r6)
      3 (get-in r6 [:a "a1"])
      1 (get-in r6 [:e "e1"])
      ) ) )

(deftest test-merge-value-frequency
  (let [m1 {:a {'x' 2}}
        m2 {:a {'x' 4} :b {'y' 2}}
        r1 (merge-value-frequencies m1 m2)
        ]
    (are [x y] (= x y)
      2 (count r1)
      6 (get-in r1 [:a 'x'])
      2 (get-in r1 [:b 'y'])
      ) ) )

(def mvs [{:a "a1" :b "b1" :c "c1"} {:a "a2" :b "b2"} {:a "a2" :b "b3" :c "c2" :d "d1"}])

(deftest test-collect-value-frequencies
  (let [r1 (collect-value-frequencies mvs)
        r2 (collect-value-frequencies mvs :kpath [:b])
        r3 (collect-value-frequencies mvs :kset [:a :c])
        r4 (collect-value-frequencies mvs :plevel 2)
        r5 (collect-value-frequencies mvs :kpath [:b] :plevel 2)
        r6 (collect-value-frequencies mvs :kset [:a :c] :plevel 2)
        ]
    (are [x y] (= x y)
      4 (count r1)
      1 (get-in r1 [:d "d1"])
      1 (get-in r1 [:c "c2"])
      1 (get-in r1 [:c "c1"])
      1 (get-in r1 [:b "b3"])
      1 (get-in r1 [:b "b2"])
      1 (get-in r1 [:b "b1"])
      2 (get-in r1 [:a "a2"])
      1 (get-in r1 [:a "a1"])
      ; r2
      0 (count r2)
      ; r3
      2 (count r3)
      1 (get-in r3 [:c "c2"])
      1 (get-in r3 [:c "c1"])
      2 (get-in r3 [:a "a2"])
      1 (get-in r3 [:a "a1"])
      ;
      r4 r1
      r5 r2
      r6 r3
      ) ) )

(deftest test-sort-value-frequencies
  (let [r1 (sort-value-frequencies {:a {"a1" 2 "a2" 5 "a3" 1}})
        r2 (sort-value-frequencies (collect-value-frequencies mvs))]

    (are [x y] (= x y)
      '(5 2 1) (vals (get-in r1 [:a]))
      '(2 1) (vals (get-in r2 [:a]))
      '(1 1 1) (vals (get-in r2 [:b]))
      ) ) )

(def m1 {:a "a1" :b {:c [{:d "d1"} {:d "d1"} {:d "d2"}]}})
(def m2 {:a {:e [{:f "f1"} {:f "f2"} {:f "f2"}]}})

(deftest test-collect-value-frequencies-for
  (let [r1 (collect-value-frequencies-for [m1] #(get-in % [:b :c]))
        r2 (collect-value-frequencies-for [m1 m2] #(concat (get-in % [:b :c]) (get-in % [:a :e])))
        r3 (collect-value-frequencies-for [m1] #(get-in % [:b :c]) :plevel 2)
        r4 (collect-value-frequencies-for [m1 m2] (fn [m] (mapcat #(get-in m %) '([:b :c] [:a :e]))) :plevel 2)
        ]

    (are [x y] (= x y)
      {"d2" 1 "d1" 2} (:d r1)
      {"d2" 1 "d1" 2} (:d r2)
      {"f2" 2 "f1" 1} (:f r2)
      r1 r3
      r2 r4
      ) ) )

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