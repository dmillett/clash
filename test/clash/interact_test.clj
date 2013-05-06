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


(def simple-file (str tresource "/simple-structured.log"))

(def simple-stock-structure [:trade_time :action :stock :quantity :price])

(defn simple-stock-message-parser
  [line]
  (let [splitsky (tt/split-with-regex line #"\|")
        date (first splitsky)
        message (last splitsky)
        corrected (str date "," message)]
    (tt/text-structure-to-map corrected #"," simple-stock-structure)) )

; 05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00
(def detailed-stock-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn better-stock-message-parser
  [line]
  (tt/regex-group-into-map line simple-stock-structure detailed-stock-pattern) )

(defn is-buy-or-sell?
  [line]
  (if (or (tt/str-contains? line "Buy") (tt/str-contains? line "Sell"))
    true
    false) )


(deftest test-atomic-map-from-file
  (let [result (latency (atomic-map-from-file simple-file is-buy-or-sell? nil simple-stock-message-parser nil)
                     "map latency")
        data (deref (-> result :result))]
    ;(println (-> result :latency_text))
    (is (= 6 (count data)))
    ) )

(deftest test-atomic-list-from-file
  (let [result1 (latency (atomic-list-from-file simple-file is-buy-or-sell? nil simple-stock-message-parser)
                 "list latency")
        data1 (deref (-> result1 :result))
        result2 (atomic-list-from-file simple-file simple-stock-message-parser)]

    (is (= 6 (count data1)))
    ) )

;; Note the count is 8 instead of 6 because the 'parser' function is more specific
(deftest test-atomic-list-from-fil__2_parameters_better_parser
  (let [result1 (atomic-list-from-file simple-file simple-stock-message-parser)
        result2 (atomic-list-from-file simple-file better-stock-message-parser)]

    (is (= 8 (count @result1)))
    (is (= 6 (count @result2)))
    ) )