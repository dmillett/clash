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
        [clash.command]
        [clash.command_test]
        [clash.tools]
        [clash.csv]
        [clojure.java.io :only (delete-file)]) )

;; For more examples, see stock_example_test

(defn local-test-resource
  [f]
  (str (System/getProperty "user.dir") "/test/resources/" f))

(def regex_input (str tresource "/regex.txt"))
(def regex_output (str tresource "/java-regex-output.txt"))
;(def awk_regex (str "cat " regex_input "| awk 'match($0, /,(\\"MoreInfo\\":.*),\\"Order/, m) { print m[1] }' > awk-regex-output.txt"))
(defn regex1
  [text]
  (let [[_ m] (re-find #",(\\\"MoreInfo\\\":.*),\\\"Order" text)] m))

(deftest test-disect
  (disect regex_input regex_output :fx regex1)
  (with-jproc-dump (str "wc " regex_output " | awk '{print $3}'") " from wc of java-regex-output.txt" str)
  (delete-file regex_output))

(deftest test-transduce-csv
  (let [csv1 (transform-lines (local-test-resource "simple1.csv") csv-parse1)
        csv2 (transform-lines (local-test-resource "simple2.csv") csv-parse2
                              :joinfx (stateful-join :header? true)
                              :initv {})
        csv3 (transform-lines (local-test-resource "simple3.csv") csv-parse2
                              :joinfx (stateful-join :header? true)
                              :initv {})
        ]
    (is (= [["1" "\"true\"" " Some simple word"] ["2.0" "false" " \"Another simple word.\""]] csv1))
    (is (= [{"Column A" 1, " columnB" " \"true\"", " Column-C" " Some simple word"}
            {"Column A" 2, " columnB" " false", " Column-C" " \"Another simple word.\""}] (:result csv2)))
    (is (= [{"hotel" 150495, "txnTim" "\"foo\"", "rateCode" "HYHWCB", "roomCode" "AQSS", "exception" "Rate not found", "count" 348}] (:result csv3)))

    ) )