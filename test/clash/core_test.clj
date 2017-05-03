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
        [clojure.java.io :only (delete-file)]) )

;; For more examples, see stock_example_test

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