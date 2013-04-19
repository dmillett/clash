;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.tools_test
  (:use [clash.tools]
        [clojure.test]
        [clash.text_tools]))

(deftest test-formatf
  (is (= "2.00" (formatf 2 2)))
  (is (= "2.30" (formatf 2.3 2)))
  (is (= "1.5000" (formatf 1.5 4))) )

(deftest test-elapsed
  (is (= "t1 Time(ns):100" (elapsed 100 "t1" 0)))
  (is (= "t2 Time(ms):1.000" (elapsed 1000000 "t2" 4)))
  (is (= "t3 Time(s):1.000" (elapsed 1000000000 "t3" 4))) )

(deftest test-str-contains
   (is (not (str-contains? nil "o")))
   (is (not (str-contains? "foo" nil)))
   (is (not (str-contains? "foo" "g")))
   (is (str-contains? "foo" "o"))
   (is (not (str-contains? "foo" "|"))) )
