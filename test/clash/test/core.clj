(ns clash.test.core
  (:use [clash.core])
  (:use [clojure.test]))

(use '[clojure.java.io :only(reader delete-file)])

;; Test tools
;; todo: build macro for performance execution wrapping
(defn milis
  [start]
  (* (- (System/nanoTime) start) (Math/pow 10.0 -6.0)))

(defn count-file-lines
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq  rdr))) )

(def tresource
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
 
(deftest test-jprocess-and-write
  (is (= 4 (count-file-lines input1)))
  (let [t1 (System/nanoTime)]
    (is (nil? (jprocess-and-write command1 output1 "\n")))
    (println "clojure + grep + decoded (" output1 ") Time(ms):" (milis t1)))
  (is (= 3 (count-file-lines output1)))
  ; cleanup
  (delete-file output1) )

