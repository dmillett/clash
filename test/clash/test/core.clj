(ns clash.test.core
   (:use [clash.core])
   (:use [clojure.test]))

(def tresource
  (str (System/getProperty "user.dir") "/test/clash/test"))

;; todo: build macro for performance execution wrapping
(defn milis
  [start]
  (* (- (System/nanoTime) start) (Math/pow 10.0 -6.0)))

(deftest test-str-contains
   (is (not (str-contains? nil "o")))
   (is (not (str-contains? "foo" nil)))
   (is (not (str-contains? "foo" "g")))
   (is (str-contains? "foo" "o"))
   (is (not (str-contains? "foo" "|"))) )

(deftest test-jsystem-cmd-prefix
  (is (= 3 (count (jsystem-cmd-prefix "foo|bar"))))
  (is (= 6 (count (jsystem-cmd-prefix "foobar")))) )

(def file1 (str tresource "/file1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep ORB " file1 " | logdecode"))
 
(deftest test-jsystem-cmd
  (let [t1 (System/nanoTime)]
    (is (nil? (jsystem-cmd command1 output1)))
    (println "clojure + grep + decoded (output3.txt) Time(ms):" (milis t1))) )

