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

(deftest test-map-freqs
  (let [r1 (map-freqs mps1 [] [:a])
        r2 (map-freqs mps2 [:foo] [:b])
        ;r3 (map-freqs2 mps1 [] [])
        ]
    ;(print r3)
    (are [x y] (= x y)
      nil (-> r1 :b)
      nil (-> r1 :c)
      {3 1, 1 1} (-> r1 :a)

      nil (-> r2 :a)
      nil (-> r2 :c)
      {"y" 1 "yy" 1} (-> r2 :b)
      ) ) )

(def mvs [{:a "a1" :b "b1" :c "c1"} {:a "a2" :b "b2"} {:a "a2" :b "b3" :c "c2" :d "d1"}])

(deftest test-value-frequencies-for-map
  (let [r1 (value-frequencies-for-map {:a "a1" :b "b1"})
        r2 (value-frequencies-for-map {:a {"a1" 3}} {:a "a1" :b "b1"})]
    (are [x y] (= x y)
      1 (get-in r1 [:a "a1"])
      1 (get-in r1 [:b "b1"])
      1 (get-in r2 [:b "b1"])
      4 (get-in r2 [:a "a1"])
      ) ) )
