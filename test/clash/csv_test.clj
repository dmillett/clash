;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.csv-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [clash.csv :refer :all]
            [clash.tools :as ct]
            ))

;; todo: fix for defrecord
(def complex1a "Date,Cases - Total,Deaths - Total,Hospitalizations - Total,Cases - Age 0-17,Cases - Age 18-29,Cases - Age 30-39,Cases - Age 40-49,Cases - Age 50-59,Cases - Age 60-69,Cases - Age 70-79,Cases -  Age 80+,Cases - Age Unknown,Cases - Female,Cases - Male,Cases - Unknown Gender,Cases - Latinx,Cases - Asian Non-Latinx,Cases - Black Non-Latinx,Cases - White Non-Latinx,Cases - Other Race Non-Latinx,Cases - Unknown Race/Ethnicity,Deaths - Age 0-17,Deaths - Age 18-29,Deaths - Age 30-39,Deaths - Age 40-49,Deaths - Age 50-59,Deaths - Age 60-69,Deaths - Age 70-79,Deaths - Age 80+,Deaths - Age Unknown,Deaths - Female,Deaths - Male,Deaths - Unknown Gender,Deaths - Latinx,Deaths - Asian Non-Latinx,Deaths - Black Non-Latinx,Deaths - White Non-Latinx,Deaths - Other Race Non-Latinx,Deaths - Unknown Race/Ethnicity,Hospitalizations - Age 0-17,Hospitalizations - Age 18-29,Hospitalizations - Age 30-39,Hospitalizations - Age 40-49,Hospitalizations - Age 50-59,Hospitalizations - Age 60-69,Hospitalizations - Age 70-79,Hospitalizations - Age 80+,Hospitalizations - Age Unknown,Hospitalizations - Female,Hospitalizations - Male,Hospitalizations - Unknown Gender,Hospitalizations - Latinx,Hospitalizations - Asian Non-Latinx,Hospitalizations - Black Non-Latinx,Hospitalizations - White Non-Latinx,Hospitalizations - Other Race Non-Latinx,Hospitalizations - Unknown Race/Ethnicity\n")
(def complex1b "06/21/2020,47,11,18,4,16,11,3,7,2,3,1,0,28,19,0,16,1,10,6,2,12,0,0,1,0,1,3,3,3,0,6,5,0,4,0,3,4,0,0,1,2,2,1,3,3,5,1,0,13,5,0,8,0,7,2,1,0")

;; todo: record with data breaks???
(deftest test-keyfx
  (let [header1 (csv-parse1 (clean-keys complex1a))
        data1 (csv-parse1 complex1b)]
    (are [x y] (= x y)
      nil (keyfx nil)
      nil (keyfx "")
      {"a" 1 "b" 2 "c" 3} ((keyfx ["a" "b" "c"]) [1 2 3])
      {"a" 1 "b" 2 "c" 3} ((keyfx ["a" "b" "c"] :recname nil) [1 2 3])
      {:a 1 :b 2 :c 3} ((keyfx ["a" "b" "c"] :keywords? true) [1 2 3])
      nil (keyfx nil :recname "Foo")
      nil (keyfx "" :recname "Foo")
      "class user.Foo" (str (type ((keyfx ["a" "b" "c"] :recname "Foo") [4 5 6])))
      '(4 5 6) (vals ((keyfx ["a" "b" "c"] :recname "Foo") [4 5 6]))
       (count header1) (count data1)
       58 (count ((keyfx header1 :recname "Zoo") data1))
       ) ) )

(deftest test-csv-parse1
  (are [x y] (= x y)
    nil (csv-parse1 nil)
    [""] (csv-parse1 "")
    ["foo"] (csv-parse1 "foo")
    ["a" "b" "c"] (csv-parse1 "a,b,c")
     ))

(deftest test-csv-parse2
  (are [x y] (= x y)
     nil (csv-parse2 nil)
     [""] (csv-parse2 "")
     ["foo"] (csv-parse2 "foo")
     ["a" "b" "c"] (csv-parse2 "a,b,c")
     [1 2 3] (csv-parse2 "1,2,3")
     [1.0 "a" -3.1 nil] (csv-parse2 "1.0,a,-3.1,nil")
     ))

(deftest test-clean-keys
  (are [x y] (= x y)
    "a,b,c" (clean-keys "a,b,c")
    "a,_b,c" (clean-keys " a,-b,c+")
    "_a_,_b_,c_" (clean-keys "[a],(-b),c|")
    ))


(def foo ["Timestamp,Transaction Type,Asset,Quantity Transacted,USD Spot Price at Transaction,USD Subtotal,USD Total (inclusive of fees),USD Fees,Notes\n"
          "2020-12-06T20:02:52Z,Buy,BTC,0.00026156,19150.00,5.01,5.01,0.00,Bought 0.00026156 BTC for $5.01 USD"
          "2020-12-16T16:01:24Z,Buy,BTC,0.02365302,20828.63,492.66,500.00,7.34,Bought 0.02365302 BTC for $500.00 USD"
          "2020-12-16T16:02:04Z,Buy,ETH,0.78435942,628.10,492.66,500.00,7.34,Bought 0.78435942 ETH for $500.00 USD"])

(deftest test-stateful-join
  (let [tdfx1 (comp identity (map csv-parse1))
        tdfx2 (comp identity (map csv-parse2))
        td1 (transduce tdfx1 (stateful-join) ["a,b,c" "1,2,3"])
        td2 (transduce tdfx1 (stateful-join :header? true) ["a,b,c" "1,2,3" "4,5,6"])
        td3 (transduce tdfx1 (stateful-join :header? true) ["a,b,c" "1,2" "4,5,6" "7,8,9,10"])
        td4 (transduce tdfx1 (stateful-join :header? true :recname "Foo1") ["a,b,c" "1,2,3" "4,5,6"])
        td5 (transduce tdfx1 (stateful-join :header? true :recname "Foo2") ["a,b,c" "1,2" "4,5,6" "7,8,9,10"])
        td6 (transduce tdfx2 (stateful-join :header? true :recname "Foo3") ["a,b,c" "1,2" "4,5,6" "7,8,9,10"])
        td7 (transduce tdfx2 (stateful-join :header? true :recname "Chicago" :kclean clean-keys) [complex1a complex1b])
        td8 (transduce tdfx2 (stateful-join :header? true :kclean clean-keys) [complex1a complex1b])
        td9 (transduce tdfx2 (stateful-join :header? true :kclean clean-keys) foo)
        td10 (transduce tdfx1 (stateful-join :header? true :recname "Foo" :kclean clean-keys) foo)
        ]
    (are [x y] (= x y)
       [["a" "b" "c"] ["1" "2" "3"]] (:result td1)
       [{"a" "1" "b" "2" "c" "3"} {"a" "4" "b" "5" "c" "6"}] (:result td2)
       [{"a" "1" "b" "2"} {"a" "4" "b" "5" "c" "6"} {"a" "7" "b" "8" "c" "9"}] (:result td3)
       "class user.Foo1" (str (type (first (:result td4))))
       "class user.Foo1" (str (type (last (:result td4))))
       [["1" "2" nil] ["4" "5" "6"] ["7" "8" "9"]] (map vals (:result td5))
       [[1 2 nil] [4 5 6] [7 8 9]] (map vals (:result td6))
       58 (count (first (:result td7)))
       47 (:Cases_Total (first (:result td7)))
       58 (count (first (:result td8)))
       47 (get (first (:result td8)) "Cases_Total")
       3 (count (:result td9))
       3 (count (:result td10))
       ) ) )

