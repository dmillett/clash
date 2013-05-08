;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.interact_test
  (:require [clash.text_tools :as tt])
  (:use [clojure.test]
        [clash.interact]
        [clash.command_test]
        [clash.tools]) )

;; ********************** Example Block ***********************************

(def simple-file (str tresource "/simple-structured.log"))

(def simple-stock-structure [:trade_time :action :stock :quantity :price])

; 05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00
(def detailed-stock-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn is-buy-or-sell?
  "If the current line contains 'Buy' or 'Sell'. "
  [line]
  (if (or (tt/str-contains? line "Buy") (tt/str-contains? line "Sell"))
    true
    false) )

(defn simple-stock-message-parser
  "An inexact split and parse of line text into 'simple-stock-structure'."
  [line]
  (let [splitsky (tt/split-with-regex line #"\|")
        date (first splitsky)
        message (last splitsky)
        corrected (str date "," message)]
    (tt/text-structure-to-map corrected #"," simple-stock-structure)) )

(defn better-stock-message-parser
  "An exact parsing of line text into 'simple-stock-structure' using
  'detailed-stock-pattern'."
  [line]
  (tt/regex-group-into-map line simple-stock-structure detailed-stock-pattern) )

;; ****************************************************************************************
;; ****************************************************************************************

(deftest test-atomic-map-from-file
  (let [result (atomic-map-from-file simple-file is-buy-or-sell? nil simple-stock-message-parser nil)]

    (is (= 6 (count @result)))
    ) )

(deftest test-atomic-list-from-file
  (let [result1 (atomic-list-from-file simple-file is-buy-or-sell? nil simple-stock-message-parser)]

    (is (= 6 (count @result1)))
    ) )

;; Note the count is 8 instead of 6 because the 'parser' function is more specific
(deftest test-atomic-list-from-fil__2_parameters_better_parser
  (let [result1 (atomic-list-from-file simple-file simple-stock-message-parser)
        result2 (atomic-list-from-file simple-file better-stock-message-parser)]

    (is (= 8 (count @result1)))
    (is (= 6 (count @result2)))
    ) )

(defn stock-name?
  "A predicate to check 'stock' name against the current solution."
  [stock]
  #(= stock (-> % :stock)))

(defn stock-name-action?
  "A predicate to check 'stock' name and 'action' against the current solution."
  [stock action]
  #(and (= stock (-> % :stock)) (= action (-> % :action))) )

(defn stock-name-action-every-pred?
  "A predicate to check 'stock' name and 'action' against the current solution,
  using 'every-pred'."
  [stock action]
  (every-pred (stock-name? stock) #(= action (-> % :action))) )

(def increment-with-stock-quanity
  "Destructures 'solution' and existing 'count', and adds the stock 'quantity'
   'count'."
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )

(deftest test-count-with-conditions
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    (are [x y] (= x y)
      0 (count-with-conditions @solutions #(= "XYZ" (-> % :stock)))
      6 (count-with-conditions @solutions nil)
      3 (count-with-conditions @solutions #(= "FOO" (-> % :stock)))
      3 (count-with-conditions @solutions (stock-name? "FOO"))
      2 (count-with-conditions @solutions (stock-name-action? "FOO" "Buy"))
      1 (count-with-conditions @solutions (stock-name-action-every-pred? "FOO" "Sell"))
      ) ) )

;; Demonstrating custom increment
(deftest test-count-with-conditions__with_incrementer
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    (are [x y] (= x y)
      3 (count-with-conditions @solutions (stock-name? "FOO") 0)
      1200 (count-with-conditions @solutions (stock-name? "FOO") increment-with-stock-quanity 0)
      2470 (count-with-conditions @solutions nil increment-with-stock-quanity 20)
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
    0 (count-with-conditions medium_complexity (is-zoo? "PIG"))
    1 (count-with-conditions medium_complexity (is-zoo? "ZOO"))
    1 (count-with-conditions medium_complexity is-fur-odd?)
    0 (count-with-conditions medium_complexity (every-pred is-fur-odd? (is-zoo? "BAR")))
    1 (count-with-conditions medium_complexity (every-pred is-fur-odd? (is-zoo? "ZAP")))
    ) )

;; Collecting results
(deftest test-collect-with-conditions
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    (are [x y] (= x y)
      0 (count (collect-with-condition @solutions (stock-name? "XYZ")) )
      1 (count (collect-with-condition @solutions (stock-name-action-every-pred? "FOO" "Sell")))
      2 (count (collect-with-condition @solutions (stock-name-action? "FOO" "Buy")))
      6 (count (collect-with-condition @solutions nil))
      ) ) )
