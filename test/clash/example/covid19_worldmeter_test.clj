(ns clash.example.covid19_worldmeter_test
  (:require [clojure.test :refer :all]
            [clash.core :as cc])
  (:use clash.example.covid19_worldmeter))

(def text0 "Nebraska \t412 \t+49 \t8 \t\t404 \t216 \t4 \t6,883 \t3,614 \t[1] [2] [3")
(def text1 "Wyoming \t212 \t+12 \t\t\t160 \t364 \t\t3,929 \t6,753 \t[1] [2]")
(def text2 "New York \t131,916 \t+8,898 \t4,758 \t+599 \t113,792 \t6,724 \t243 \t320,811 \t16,353 \t[1] [2] [3] [4] [5] [6] [7] [8]")

(deftest test-wm-cleanup
  (are [x y] (= x y)
   "Nebraska ,412 ,49 ,8 ,,404 ,216 ,4 ,6883 ,3614 ,[1] [2] [3" (wm-cleanup text0)
   "Wyoming ,212 ,12 ,,,160 ,364 ,,3929 ,6753 ,[1] [2]" (wm-cleanup text1)
    "New York ,131916 ,8898 ,4758 ,599 ,113792 ,6724 ,243 ,320811 ,16353 ,[1] [2] [3] [4] [5] [6] [7] [8]" (wm-cleanup text2)
    ))

(deftest test-wm-parser
  (is (= #clash.example.covid19_worldmeter.CovidData{:state "New York", :total_pos 131916, :new_cases 8898, :deaths 4758, :new_deaths 599, :active 113792, :cases_million 6724, :deaths_million 243, :test_count 320811, :tests_million 16353, :sources "[1] [2] [3] [4] [5] [6] [7] [8]"} (wm-parser text2)))
  )

(def nygradients
  {:deltas [0.13102018471242305 0.102404325694331 0.08834192064643576 0.07218878473577717 0.07925310676583197 0.07681662617110718 0.06905446445712782],
   :gradients [0.13102018471242305 0.78159198080127 0.8626776266280934 0.8171520859807081 1.0978589964620042 0.9692569705573332 0.8989520615408266]})

(deftest test+values
  (is (= 5 (+values [2 nil 3]))))

(deftest test++values
  (is (= [3 0 4] (++values [[1 nil 2] [2 0 2]]))))

(deftest test*values
  (is (= 6 (*values [2 nil 3]))))

(deftest test**values
  (is (= [3 8 1] (**values [[1 2 nil][3 4 nil]]))))

(deftest test-sort-values
  (are [x y] (= x y)
    5 (sort-values [2 nil 3] + :dvfx #(replace {nil 0} %))
    8 (sort-values [2 nil 4] * :dvfx #(replace {nil 1} %))
    [3 0 4] (sort-values [[1 nil 2] [2 0 2]] + :dvfx (fn [coll] (map #(replace {nil 0} %) coll)))
    [3 8 1] (sort-values [[1 2 nil] [3 4 1]] * :dvfx (fn [coll] (map #(replace {nil 1} %) coll)))
    ))

(deftest test-wm-population
  (is (= 1000000 (wm-population 30 30)))
  (is (nil? (wm-population nil 30)))
  (is (nil? (wm-population 30 nil)))
  )

(deftest test-wm-percentage
  (is (nil? (wm-percentage nil 10)))
  (is (nil? (wm-percentage 100 nil)))
  (is (= 10.0 (wm-percentage 100 10)))
  (is (= 0.1 (wm-percentage 100 10 :percent false)))
  )

(deftest test-compare-values
  (let [d1 {:a 1 :b 2}
        d2 {:a 2 :b 1}]

    (is (= {:a 2 :b 2} (compare-values d1 [:a :b] d2 >)))
    (is (= d1 (compare-values d1 [:a :b] {} >)))
    (is (= d2 (compare-values {} [:a :b] d2 >)))
    (is (empty? (compare-values nil nil {} <)))
    ) )

(deftest test-merge-percentages
  (let [d1 {:a {:b 1 :c 2}}
        d2 {:a {:b 2 :c 3}}]
    (is (= {:a {:b [1 2], :c [2 3]}} (merge-percentages d1 d2)))
    ) )

(deftest test-deltas
  (is (= {:deltas [3.0 5.0], :gradients [3.0 1.6666666666666667]} (deltas (take 3 [1 4 9 20 32]))))
  (is (= {:deltas [3.0 5.0 11.0 12.0], :gradients [3.0 1.6666666666666667 2.2 1.0909090909090908]} (deltas [1 4 9 20 32]))))