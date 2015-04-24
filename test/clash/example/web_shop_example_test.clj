;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.example.web_shop_example_test
  (:require [clash.text_tools :as tt])
  (:use [clojure.test]
        [clash.example.web_shop_example]
        [clash.core]
        [clash.pivot]
        [clash.command_test]
        [clash.tools]) )

(def web-log-file (str tresource "/web-shop.log"))

(defn is-search-or-price?
  "If the current line contains 'Search' or 'Price'. "
  [line]
  (if (or (tt/str-contains? line "Search") (tt/str-contains? line "Price"))
    true
    false) )

(deftest test-atomic-map-from-file
  (let [result1 (atomic-map-from-file web-log-file into-memory-parser 4)
        result2 (atomic-map-from-file web-log-file is-search-or-price? nil into-memory-parser nil -1)]

    (is (= 4 (count @result1)))
    ; todo: check on this -- Purchase line fits the pattern, but because of predicate, it's 'nil'
    (is (= 7 (count @result2)))
    ) )

(deftest test-atomic-list-from-file
  (let [result1 (atomic-list-from-file web-log-file into-memory-parser 3)
        result2 (atomic-list-from-file web-log-file is-search-or-price? nil into-memory-parser -1)]

    (is (= 3 (count @result1)))
    (is (= 6 (count @result2)))
    ) )

;;; Note the count is 8 instead of 6 because the 'parser' function is more specific
(deftest test-atomic-list-from-file__2_parameters_better_parser
  (let [result (atomic-list-from-file web-log-file into-memory-parser)]

    (is (= 7 (count @result)))
    ) )

(deftest test-file-into-structure
  (let [vector_result (file-into-structure web-log-file into-memory-parser [])
        list_result (file-into-structure web-log-file into-memory-parser '())]

    (are [x y] (= x y)
      clojure.lang.PersistentVector (type vector_result)
      7 (count vector_result)
      clojure.lang.PersistentList (type list_result)
      7 (count list_result)
      ) ) )

(deftest test-count-with-conditions
  (let [solutions (atomic-list-from-file web-log-file into-memory-parser)]
    ;(println (first @solutions))
    (are [x y] (= x y)
      0 (s-count-with @solutions #(= "XYZ" (-> % :name)))
      7 (s-count-with @solutions nil)
      3 (s-count-with @solutions #(= "FOO" (-> % :name)))
      3 (s-count-with @solutions (name? "FOO"))
      2 (s-count-with @solutions (name-action? "FOO" "Search"))
      ; any? and all?
      2 (s-count-with @solutions (all? (name? "FOO") (any? (action? "Search") (action? "Purchase"))) )
      4 (s-count-with @solutions (all? (price-higher? 13.00) (price-lower? 17.00)))
      ) ) )

;; Demonstrating custom increment
(deftest test-count-with-conditions__with_incrementer
  (let [solutions (atomic-list-from-file web-log-file into-memory-parser)]
    (are [x y] (= x y)
      3 (s-count-with @solutions (name? "FOO") 0)
      9 (s-count-with @solutions (name? "FOO") increment-with-quanity 0)
      ; count all quantities regardless of search, price, purchase
      84 (s-count-with @solutions nil increment-with-quanity 20)
      ) ) )

;; Collecting results
(deftest test-collect-with-conditions
  (let [solutions (atomic-list-from-file web-log-file into-memory-parser)]
    (are [x y] (= x y)
      0 (count (collect-with @solutions (name? "XYZ")) )
      2 (count (collect-with @solutions (name-action? "FOO" "Search")))
      7 (count (collect-with @solutions nil))
      ) ) )

; edn file created with (data-to-file)
(deftest test-write-data-to-edn-and-read-back
  (let [solutions (file-into-structure web-log-file into-memory-parser [])
        web_solutions_file (str tresource "/web-solutions")
        solutions_from_file (data-from-file (str web_solutions_file ".edn"))]
    (is (= solutions solutions_from_file))
    ) )