;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.command_test
  (:use [clash.command]
        [clojure.java.io :only (reader delete-file)]
        [clash.tools]
        [clash.text_tools]
        [clojure.test]))

(def tresource
  "Define the current test directory."
  (str (System/getProperty "user.dir") "/test/resources"))

(deftest test-pipe
  (is (= 3 (count (pipe "foo|bar"))))
  (is (= 6 (count (pipe "foobar"))))
  )

; Test simple grep
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

; Commented out to reduce console spam
(comment 
(deftest test-jproc-dump
  (perf (jproc-dump command1 "") "console (dump) test")
  ))

; Using (perf) instead of (time)
(deftest test-jproc-write
  (is (= 4 (count-file-lines input1)))
  (perf (jproc-write command1 output1 "\n") "'cl + grep' results to file")
  (is (= 3 (count-file-lines output1)))
  (delete-file output1)
  )

(def output2 (str tresource "/output2.txt"))

(deftest test-jproc-write-grep-cut
  (let [c1 (str "grep message " input1 " | cut -d\",\" -f2 " input1)
        c2 (str "wc -l")
        out2 (str tresource "/output2.txt")]

    (is (= 4 (count-file-lines input1)))
    (perf (jproc-write c1 out2 ":") "'cl + grep + cut' results to file")
    (is (= 1 (count-file-lines out2)))
    ; To see results comment out the next line "is:this: hopefully: satire?"
    (delete-file out2)
  ) )

(def command3 (str "grep hopefully " input1))
(def output3 (str tresource "/output3.txt"))

(deftest test-with-jproc
  (is (= 4 (count-file-lines input1)))
  (with-jproc command3 "" output3 last)
  (is (= 1 (count-file-lines output3)))
  ; To see results comment out the next line "y"
  (delete-file output3)
  )

; Commented out to reduce console spam
(comment
(deftest test-with-jproc-dump
  (with-jproc-dump command2 ":" last)
  ))