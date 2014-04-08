;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.core_test
  (:require [clash.text_tools :as tt])
  (:use [clojure.test]
        [clash.core]
        [clash.command_test]
        [clash.tools]) )

;; For more examples, see stock_example_test

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

(deftest test-with-all-predicates
  (let [result1 (all-preds? 5 even?)
        result2 (all-preds? 4 even?)
        result3 (all-preds? 4 number? even?)
        result4 (all-preds? 4 number? odd?)
        result5 (all-preds? 12 number? even? #(= 0 (mod % 6)))]
    (is (not result1))
    (is result2)
    (is result3)
    (is (not result4))
    (is result5)
    ) )

(deftest test-with-any-predicates
  (let [result1 (any-preds? 5 even?)
        result2 (any-preds? 4 even?)
        result3 (any-preds? 4 number? even?)
        result4 (any-preds? 4 number? odd?)
        result5 (any-preds? 12 number? even? #(= 0 (mod % 5)))]
    (is (not result1))
    (is result2)
    (is result3)
    (is result4)
    (is result5)
    ) )

(defn divide-by-x?
  [x]
  #(= 0 (mod % x)) )

(deftest any-and-all?
  (let [result1 ((all? number? even?) 10)
        result2 ((all? number? odd?) 10)
        result3 ((any? number? even?) 11)
        result4 ((all? number? even? (divide-by-x? 5)) 10)
        result5 ((any? number? odd? even?) 16)
        result5 ((all? number? (any? (divide-by-x? 6) (divide-by-x? 4))) 16)]
    (is result1)
    (is (not result2))
    (is result3)
    (is result4)
    (is result5)
    )
  )