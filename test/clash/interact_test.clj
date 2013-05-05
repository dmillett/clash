;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.interact_test
  (:use [clojure.test]
        [clash.interact]
        [clash.text_tools]
        [clash.command_test]) )


(def simple-file (str tresource "/simple-structured.log"))

(def simple-stock-structure [:trade_time :action :stock :quantity :price])

(defn simple-stock-message-parser
  [line]
  (let [splitsky (split-with-regex line #"\|")
        date (first splitsky)
        message (last splitsky)
        corrected (str date "," message)]
    (text-structure-to-map corrected #"," simple-stock-structure)) )

(defn is-buy-or-sell?
  [line]
  (if (or (str-contains? line "Buy") (str-contains? line "Sell"))
    true
    false) )


(deftest test-atomic-map-from-file
  (let [result (atomic-map-from-file simple-file is-buy-or-sell? nil simple-stock-message-parser)]
    ;(println @result)
    (is (= 6 (count @result)))
    ) )

