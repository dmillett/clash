;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.example.stock_example_test
  (:require [clash.text_tools :as tt])
  (:use [clojure.test]
        [clash.example.stock_example]
        [clash.interact]
        [clash.command_test]
        [clash.tools]) )

(def simple-file (str tresource "/simple-structured.log"))

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

(deftest test-count-with-conditions
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    ;(println solutions)
    (are [x y] (= x y)
      0 (count-with-conditions @solutions #(= "XYZ" (-> % :stock)))
      6 (count-with-conditions @solutions nil)
      3 (count-with-conditions @solutions #(= "FOO" (-> % :stock)))
      3 (count-with-conditions @solutions (name? "FOO"))
      2 (count-with-conditions @solutions (name-action? "FOO" "Buy"))
      1 (count-with-conditions @solutions (name-action-every-pred? "FOO" "Sell"))
      ; any? and all?
      3 (count-with-conditions @solutions (all? (name? "FOO") (any? (action? "Sell") (action? "Buy"))) )
      1 (count-with-conditions @solutions (all? (name? "FOO") (price-higher? 12.1) (price-lower? 12.7)))
      ) ) )

;; Demonstrating custom increment
(deftest test-count-with-conditions__with_incrementer
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    (are [x y] (= x y)
      3 (count-with-conditions @solutions (name? "FOO") 0)
      1200 (count-with-conditions @solutions (name? "FOO") increment-with-stock-quanity 0)
      2470 (count-with-conditions @solutions nil increment-with-stock-quanity 20)
      ) ) )

;; Collecting results
(deftest test-collect-with-conditions
  (let [solutions (atomic-list-from-file simple-file better-stock-message-parser)]
    (are [x y] (= x y)
      0 (count (collect-with-condition @solutions (name? "XYZ")) )
      1 (count (collect-with-condition @solutions (name-action-every-pred? "FOO" "Sell")))
      2 (count (collect-with-condition @solutions (name-action? "FOO" "Buy")))
      6 (count (collect-with-condition @solutions nil))
      ) ) )