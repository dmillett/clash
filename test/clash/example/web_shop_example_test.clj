;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.example.web_shop_example_test
  (:require [clash.text_tools :as tt]
            [clojure.string :as s]
            [clojure.pprint :as pp])
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
  (if (or (s/includes? line "Search") (s/includes? line "Price"))
    true
    false) )

(deftest test-transform-lines
  (let [result1 (transform-lines web-log-file weblog-parser-dr)
        result2 (transform-lines web-log-file weblog-parser-dr :tdfx (comp (map weblog-parser-dr) (filter is-search-or-price?) (filter identity)))
        ]
    (is (= 4 (count result1)))
    (is (= 7 (count result2)))
    ) )

(deftest test-atomic-list-from-file
  (let [result1 (atomic-list-from-file web-log-file weblog-parser 3)
        result2 (atomic-list-from-file web-log-file is-search-or-price? nil weblog-parser -1)]

    (is (= 3 (count @result1)))
    (is (= 6 (count @result2)))
    ) )

;;; Note the count is 8 instead of 6 because the 'parser' function is more specific
(deftest test-atomic-list-from-file__2_parameters_better_parser
  (let [result (atomic-list-from-file web-log-file weblog-parser)]

    (is (= 7 (count @result)))
    ) )

(deftest test-file-into-structure
  (let [vector_result (file-into-structure web-log-file weblog-parser [])
        list_result (file-into-structure web-log-file weblog-parser '())]

    (are [x y] (= x y)
      clojure.lang.PersistentVector (type vector_result)
      7 (count vector_result)
      clojure.lang.PersistentList (type list_result)
      7 (count list_result)
      ) ) )

(deftest test-haystack
  (let [result (transform-lines web-log-file weblog-parser-dr)
        hstack1 (haystack result :vfkpsets [{:ks [:action :name]}] :vffx (top-freqs 2))
        hstack2 (haystack result :vfkpsets [{:ks [:action :name]}] :vffx (bottom-freqs 1))
        ]
    ;(pp/pprint result)
    (are [x y] (= x y)
      4 (count hstack1)
      2 (:count (get hstack1 "haystack([:action]|[:name])_[Search|FOO]"))
      0 (:count (get hstack2 "haystack([:action]|[:name])_[Purchase|ZOO]"))
      ) ) )

(deftest test-transform-lines
  (let [result1 (transform-lines web-log-file weblog-parser)
        result2 (transform-lines web-log-file weblog-parser :max 3)
        result3 (transform-lines web-log-file weblog-parser :max 5 :tdfx (filter identity))]
    (are [x y] (= x y)
      7 (count result1)
      "Search" (:action (first result1))
      "10" (:quantity (nth result1 6))
      2 (count result2)
      "Search" (:action (first result2))
      "5" (:quantity (first result2))
      5 (count result3)
      "05042013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00" (second result3)
      ) ) )

(deftest test-transform-lines-verbose
  (let [result1 (transform-lines-verbose web-log-file weblog-parser)
        result2 (transform-lines-verbose web-log-file weblog-parser :max 3)]
    (are [x y] (= x y)
      7 (count (:p result1))
      2 (count (:f result1))
      9 (:c result1)
      2 (count (:p result2))
      1 (count (:f result2))
      3 (:c result2)
      ) ) )

(deftest test-count-with-conditions
  (let [solutions (atomic-list-from-file web-log-file weblog-parser)]
    ;(perfd (first @solutions))
    (are [x y] (= x y)
      0 (count-with @solutions #(= "XYZ" (-> % :name)) :plevel 1)
      0 (count-with @solutions nil :plevel 1)
      7 (count-with @solutions #(not (nil? (-> % :action))) :plevel 1)
      3 (count-with @solutions #(= "FOO" (-> % :name)) :plevel 1)
      3 (count-with @solutions (name? "FOO") :plevel 1)
      2 (count-with @solutions (name-action? "FOO" "Search") :plevel 1)
      ; any? and all?
      2 (count-with @solutions (all? (name? "FOO") (any? (action? "Search") (action? "Purchase")))  :plevel 1)
      4 (count-with @solutions (all? (price-higher? 13.00) (price-lower? 17.00)) :plevel 1)
      ) ) )

;; Demonstrating custom increment
(deftest test-count-with-conditions__with_incrementer
  (let [solutions (atomic-list-from-file web-log-file weblog-parser)]
    (are [x y] (= x y)
      3 (count-with @solutions (name? "FOO") :initval 0 :plevel 1)
      9 (count-with @solutions (name? "FOO") :incrfx increment-with-quanity :initval 0 :plevel 1)
      ; count all quantities regardless of search, price, purchase
      ; 84
      0 (count-with @solutions nil :incrfx increment-with-quanity :initval 20 :plevel 1)
      29 (count-with @solutions (name? "FOO") :incrfx increment-with-quanity :initval 20 :plevel 1)
      20 (ts-count-with @solutions nil :incrfx increment-with-quanity :initval 20)
      29 (ts-count-with @solutions (name? "FOO") :incrfx increment-with-quanity2 :initval 20)
      ) ) )

;; Collecting results
(deftest test-collect-with-conditions
  (let [solutions (atomic-list-from-file web-log-file weblog-parser)]
    (are [x y] (= x y)
      0 (count (collect-with @solutions (name? "XYZ")) )
      2 (count (collect-with @solutions (name-action? "FOO" "Search")))
      0 (count (collect-with @solutions nil))
      ) ) )

; edn file created with (data-to-file)
(deftest test-write-data-to-edn-and-read-back
  (let [solutions (file-into-structure web-log-file weblog-parser [])
        web_solutions_file (str tresource "/web-solutions")
        solutions_from_file (data-from-file (str web_solutions_file ".edn"))]
    (is (= solutions solutions_from_file))
    ) )
