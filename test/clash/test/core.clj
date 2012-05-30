(ns clash.test.core
  (:use [clash.core])
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
  "How many lines in a file?"
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq  rdr))) )

(def tresource
  "Define the current test directory."
  (str (System/getProperty "user.dir") "/test/clash/test"))

;; Tests
(deftest test-str-contains
   (is (not (str-contains? nil "o")))
   (is (not (str-contains? "foo" nil)))
   (is (not (str-contains? "foo" "g")))
   (is (str-contains? "foo" "o"))
   (is (not (str-contains? "foo" "|"))) )

(deftest test-prefix-command
  (is (= 3 (count (prefix-command "foo|bar"))))
  (is (= 6 (count (prefix-command "foobar")))) )

;; Test simple grep
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

(deftest test-jproc-dump
  (perf (jproc-dump command1 "") "console (dump) test"))

;; Using (nperf) instead of (time)
(deftest test-jproc-write
  (is (= 4 (count-lines input1)))
  (perf (jproc-write command1 output1 "\n") "Small file 'cl + grep' and dump")
  (is (= 3 (count-lines output1)))
  ; cleanup
  (delete-file output1) )

  