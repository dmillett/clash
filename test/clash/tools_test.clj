;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.tools_test
  (:use [clash.tools]
        [clojure.test])
  (:require [clojure.string :as s]))

(deftest test-formatf
  (is (= "2.00" (formatf 2 2)))
  (is (= "2.30" (formatf 2.3 2)))
  (is (= "1.5000" (formatf 1.5 4))) )

(deftest test-elapsed
  (is (= "t1 Time(ns):100" (elapsed 100 "t1 " 0)))
  (is (= "t2 Time(ms):1.0000" (elapsed 1000000 "t2 " 4)))
  (is (= "t3 Time(s):1.0000" (elapsed 1000000000 "t3 " 4))) )

;; Test functions for perf and latency
(defn foobar [x] (* x x))
(defn phrase-it [result phrase] (str result phrase))

(deftest test-latency
  (let [result1 (latency (foobar 5))
        result2 (latency (phrase-it (foobar 10) ", the square of 10"))
        result3 (latency (foobar 6) "six squared")
        result4 (latency (foobar 7) "seven " "squared")]
    (are [x y] (= x y)
      25 (-> result1 :result)
      nil (-> result1 :text)
      "100, the square of 10" (-> result2 :result)
      "six squared" (-> result3 :text)
      36 (-> result3 :result)
      "seven squared" (-> result4 :text)
      49 (-> result4 :result)
      ) ) )

(deftest test-perf
  (let [r1 (perf (+ 1 1))
        r2 (perf (+ 1 2) "Simple Addition ")
        r3 (perf (+ 1 3) "Foo " "Bar ")]
    ;(println "'" r3 "'")
    (is (= 2 r1))
    (is (= 3 r2))
    ) )

(deftest test-perfd
  (let [r1 (perfd (+ 1 1))
        r2 (perfd (+ 1 2) "= 3? ")]
    (= 2 r1)
    (= 3 r2)
    ) )

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

(def mvs2 [{:a "a1" :b "b1" :c {:d "d1" :e "e1"}} {:a "a2" :b "b2"} {:a "a2" :c {:d "d1"} :f [{:g "g1"} {:g "g2"}]}])

(deftest test-mv-freqs
  (let [r1 (mv-freqs mvs)
        r2 (mv-freqs mvs :kpsets [{:kp [:b]}])
        r3 (mv-freqs mvs :kpsets [{:ks [:a :c]}])
        r4 (mv-freqs mvs :plevel 2)
        r5 (mv-freqs mvs :kpsets [{:kp [:b]}] :plevel 2)
        r6 (mv-freqs mvs :kpsets [{:ks [:a :c]}] :plevel 2)
        r7 (mv-freqs mvs2 :kpsets [{:kp [:c] :ks [:d]} {:ks [:a]}])
        r8 (mv-freqs mvs2 :kpsets [{:kp [:c] :ks [:d]}])
        ]
    (println r8)
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
     {:d {"d1" 2}, :a {"a1" 1, "a2" 2}} r7
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

(deftest test-distinct-by
  (let [c1 [{:a "a1"} {:a "a2" :b "b1"} {:c "c2" :a "a2" :b "b1"}]
        c2 [{"foo" 1} {"foo" 1 "bar" 3}]
        c3 [1 3 4 5 "six" 7 8]]
    (are [x y] (= x y)
      '({:a "a1"} {:b "b1", :a "a2"}) (distinct-by c1 #(-> % :a))
      '({:c "c2" :a "a2" :b "b1"}) (distinct-by c1 #(-> % :c))
      '({"foo" 1}) (distinct-by c2 #(get % "foo"))
      '(1) (distinct-by c3 #(and (number? %) (odd? %)))
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

(deftest test-none
  (is ((none? number?) "5"))
  (is ((none? seq? odd?) 10))
  (is (not ((none? number? even?) 10)))
  (is ((none? even? (divisible-by? 4)) 9))
  )

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
    0 (count-with medium_complexity (is-zoo? "PIG") :plevel 1)
    1 (count-with medium_complexity (is-zoo? "ZOO") :plevel 1)
    1 (count-with medium_complexity is-fur-odd? :plevel 1)
    0 (count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "BAR")) :plevel 1)
    1 (count-with medium_complexity (every-pred is-fur-odd? (is-zoo? "ZAP")) :plevel 1)
    ) )

(def foo-numbers '(2 3 4 5 9 11 12 15 20 21 25 26 27))
(def foo-numbers-mixed '(2 3 4 5 9 "a" 11 12 15 20 21 "b" 25 26 27))

(deftest test-count-with
  (let [r1 (count-with foo-numbers-mixed (all? number?))
        r2 (count-with foo-numbers-mixed (all? number? even?))
        r3 (count-with foo-numbers-mixed (all? number? even?) :initval 37 :plevel 1)
        r4 (count-with foo-numbers-mixed (all? number?) :plevel 2)
        r5 (count-with (into [] foo-numbers-mixed) (all? number?) :plevel 2)
        r6 (count-with foo-numbers-mixed (all? number? even?) :plevel 2)
        r7 (count-with (into [] foo-numbers-mixed) (all? number? even?) :plevel 2)
        r8 (count-with (into [] foo-numbers-mixed) (all? number? even?) :initval 37 :plevel 2)
        r9 (count-with {:a 1 :b 2 :c 3} even?)
        r10 (count-with {:a 1 :b 2 :c 3} even? :plevel 2)
        r11 (count-with {:a 1 :b 2 :c 3} odd? :initval 5)
        r12 (count-with {:a 1 :b 2 :c 3} odd? :initval 5 :plevel 2)
        r13 (count-with (range 0 5) odd? :initval 1 :incrfx #(+ %2 (* 2 %1)))
        r14 (count-with {:a 1 :b 2 :c 3 :d 4} even? :initval 2 :incrfx #(+ %2 (* 2 %1)) :plevel 2)]

    (are [x y] (= x y)
      13 r1
      5  r2
      42 r3
      13 r1
      r1 r4
      13 r5
      5 r6
      5 r7
      42 r8
      1 r9
      r9 r10
      7 r11
      r11 r12
      9 r13
      14 r14
      ) ) )

(deftest test-count-from-groups
  (let [groups1 ['(1 2 3) '(4 5 6) '(7 8 9)]]
    (are [x y] (= x y)
      0 (count-from-groups [] odd?)
      0 (count-from-groups nil odd?)
      5 (count-from-groups groups1 odd?)
      4 (count-from-groups groups1 even?)
      ) ) )

(deftest test-pcollect-with
  (let [r1 (collect-with foo-numbers-mixed (all? number?))
        r2 (collect-with (into [] foo-numbers-mixed) (all? number?) :plevel 2)
        r3 (collect-with (into [] foo-numbers-mixed) (all? number? even?) :plevel 2)
        r4 (collect-with (into [] foo-numbers-mixed) (all? number? even?))
        r5 (collect-with nil nil)]
    (are [x y] (= x y)
      13 (count r1)
      13 (count r2)
      5 (count r3)
      5 (count r4)
      r3 r4
      '() r5
      ) ) )

(deftest test-collect-from-groups
  (let [groups1 ['(1 2 3) '(4 5 6) '(7 8 9)]]
    (are [x y] (= x y)
      [] (collect-from-groups [] odd?)
      [] (collect-from-groups nil odd?)
      [1 3 5 7 9] (collect-from-groups groups1 odd?)
      [2 4 6 8] (collect-from-groups groups1 even?)
      ) ) )

(deftest test-data-per-thread
  (is (= 25 (data-per-thread 50 2)))
  ; due to rounding, this bumps data per thread up
  (is (<= 5 (data-per-thread 17 4)))
  ; 6 cores, not 30 results in 10 threads/core
  (is (< 2 (data-per-thread 50 30)))
  )

(deftest test-fold-conj
  (are [x y] (= x y)
    [] (fold-conj)
    [1 2] (fold-conj [1] 2)
    ))

(deftest test-format-millitime-to
  (is (= "10-24-2015" (format-millitime-to 1445715563306 "MM-dd-yyyy")))
  )

(deftest test-repeatfx
  (not (nil? (:average_time (repeatfx 5 (+ 3 3)))))
  (not (nil? (:total_time (repeatfx 5 (* 3 3)))))
  (are [x y] (= x y)
    [6 6 6] (:values (repeatfx 3 (+ 3 3) :capture true))
    50 (count (:values (repeatfx 50 (+ 4 4) :capture true)))
    nil (:values (repeatfx 2 (+ 3 3)))
    ; For Shining fans
    "All work and no play..." (first (:values (repeatfx 5000 (str "All work and no play...") :capture true)))
    ) )

(deftest test-sweetspot
  (let [r1 (sweetspot (+ 1 2))
        r2 (sweetspot (+ 2 3) :max_count 2)
        r3 (sweetspot (+ 3 4) :delta 0.50)
        r3x (take 2 (reverse (get-in r3 ["+" :results]) ))
        r4 (sweetspot (s/split "Foo Bar" #" ") :max_count 2 :delta 0.70 :verbose true)]
    (is (not (empty? (get-in r1 ["+" :results]))))
    (is (= 2 (get-in r2 ["+" :count])))
    (is (<= (Math/abs (/ (- (:average_time (second r3x)) (:average_time (first r3x))) (:average_time (second r3x)) )) 0.50))
    (is (= 10 (count (:values (first (get-in r4 ["s/split" :results])))) ))
    ) )

(deftest test-consecutive
  (let [coll1 [1 2 3 4 6 7 8 2 3 6 12 14]
        coll2 [true true false true false true true]]
    (are [x y] (= x y)
      [[2] [4 6] [8 2] [6 12 14]] (consecutive even? coll1)
      [[true true] [true] [true true]] (consecutive true? coll2)
      ) ))

(deftest test-filter-value-frequencies
  (let [freq1 {:a {"a1" 2 "a2" 1} :b {"b1" 1 "b3" 3}}]
    (are [x y] (= x y)
      freq1 (filter-value-frequencies freq1 nil)
      {:a {"a1" 2}} (filter-value-frequencies freq1 (fn [[k _]] (= "a1" k)))
      {:a {"a1" 2}} (filter-value-frequencies freq1 (fn [[k v]] (and (= "a1" k) (even? v))))
      {} (filter-value-frequencies freq1 (fn [[k v]] (and (= "a1" k) (odd? v))))
      {:a {"a2" 1}, :b {"b1" 1, "b3" 3}} (filter-value-frequencies freq1 (fn [[_ v]] (odd? v)))
      ) ) )

(deftest test-mv-freqs
  (let [data1 [{:a {:b "b1" :c "c1"} :d {:e "e1"}} {:a {:b "b1" :c "c2"}}]
        freqs1 (mv-freqs data1)
        freqs1p (mv-freqs data1 :plevel 2)
        freqs2 (mv-freqs data1 :kpsets [{:kp [:a]}])
        freqs2p (mv-freqs data1 :kpsets [{:kp [:a]}] :plevel 2)
        freqs3 (mv-freqs data1 :kpsets [{:kp [:a] :ks [:b]}])
        freqs3p (mv-freqs data1 :kpsets [{:kp [:a] :ks [:b]}])
        freqs4 (mv-freqs data1 :kpsets [{:kp [:a] :ks [:c]} {:kp [:d] :ks [:f]}])
        freqs4p (mv-freqs data1 :kpsets [{:kp [:a] :ks [:c]} {:kp [:d] :ks [:f]}])
        freqs5 (mv-freqs data1 :kpsets [{:kp [:g]}])
        freqs5p (mv-freqs data1 :kpsets [{:kp [:g]}])
        ]
    (are [x y] (= x y)
      2 (count freqs1)
      {:a {{:b "b1", :c "c1"} 1, {:b "b1", :c "c2"} 1}, :d {{:e "e1"} 1}} freqs1
      {:b {"b1" 2}, :c {"c1" 1, "c2" 1}} freqs2
      {:b {"b1" 2}} freqs3
      {:c {"c1" 1, "c2" 1}} freqs4
      {} freqs5
      freqs1p freqs1
      freqs2p freqs2
      freqs3p freqs3
      freqs4p freqs4
      freqs5p freqs5
      )))