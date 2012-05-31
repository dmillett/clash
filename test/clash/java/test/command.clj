;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.java.test.command
  (:use [clash.java.command])
  (:use [clojure.test]))

(use '[clojure.java.io :only(reader delete-file)])

;; Test tools
(defn millis
  "Convert nano seconds to milliseconds."
  [nt]
  (/ nt 1000000.0))

(defn seconds
  "Turn nano seconds into seconds."
  [nt]
  (/ (double nt) 1000000000))

(defn nano-time
  "How many nano seconds from 'start'."
  [start]
  (- (System/nanoTime) start))

(defn formatf
  "Format a number to scale. Ex: (formatf 1 3) --> 1.000"
  [number scale]
  (format (str "%." scale "f") (double number)) )

  (deftest test-formatf
    (is (= "2.00" (formatf 2 2)))
    (is (= "2.30" (formatf 2.3 2)))
    (is (= "1.5000" (formatf 1.5 4))) )
  
(defn elapsed
  "An text message with adjusted execution time (ns, ms, or s)."
  ([time message] (elapsed time message 4))
  ([time message digits]
    (cond
      (< time 99999) (str message " Time(ns):" time)
      (< time 99999999) (str message " Time(ms):" (formatf (millis time) 3))
      :else (str message " Time(s):" (formatf (seconds time) 3)))) )

(deftest test-elapsed
  (is (= "t1 Time(ns):100" (elapsed 100 "t1" 0)))
  (is (= "t2 Time(ms):1.000" (elapsed 1000000 "t2" 4)))
  (is (= "t3 Time(s):1.000" (elapsed 1000000000 "t3" 4))) )

(defmacro perf
  "Determine function execution time in nano seconds. Display is
  in nanos or millis or seconds (see elapsed())."
  [exe message]
  `(let [time# (System/nanoTime)
         result# ~exe]
     (println (elapsed (nano-time time#) ~message))
     result#))

(defn count-lines
  "How many lines in a small file?"
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq rdr))) )

(def tresource
  "Define the current test directory."
  (str (System/getProperty "user.dir") "/test/clash/java/test"))

;; Tests
(deftest test-str-contains
   (is (not (str-contains? nil "o")))
   (is (not (str-contains? "foo" nil)))
   (is (not (str-contains? "foo" "g")))
   (is (str-contains? "foo" "o"))
   (is (not (str-contains? "foo" "|"))) )

(deftest test-pipe
  (is (= 3 (count (pipe "foo|bar"))))
  (is (= 6 (count (pipe "foobar"))))
  )

;; Test simple grep
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

;; Commented out to reduce console spam
(comment 
(deftest test-jproc-dump
  (perf (jproc-dump command1 "") "console (dump) test"))
)

;; Using (perf) instead of (time)
(deftest test-jproc-write
  (is (= 4 (count-lines input1)))
  (perf (jproc-write command1 output1 "\n") "'cl + grep' results to file")
  (is (= 3 (count-lines output1)))
  (delete-file output1) )

(def command2 (str "grep message " input1 " | cut -d\",\" -f2 " input1))
(def output2 (str tresource "/output2.txt"))

(deftest test-jproc-write-grep-cut
  (is (= 4 (count-lines input1)))
  (perf (jproc-write command2 output2 ":") "'cl + grep + cut' results to file")
  (is (= 1 (count-lines output2)))
  ; To see results comment out the next line "is:this: hopefully: satire?" 
  (delete-file output2)
  )


(def command3 (str "grep hopefully " input1))
(def output3 (str tresource "/output3.txt"))

(deftest test-with-jproc
  (is (= 4 (count-lines input1)))
  (with-jproc command3 "" output3 last)
  (is (= 1 (count-lines output3)))
  ; To see results comment out the next line "y"
  (delete-file output3)
  )

;; Commented out to reduce console spam
(comment
(deftest test-with-jproc-dump
  (with-jproc-dump command2 ":" last))
)
