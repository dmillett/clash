;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.text_tools_test
  (:use [clojure.test]
        [clash.text_tools]))


(def text1 "abc")
(def text2 "ab
cd")

(deftest test-as-one-line
  (are [x y] (= x y)
    "abc" (as-one-line text1)
    "abc" (as-one-line text1 ";")
    "abc" (as-one-line text1 nil)
    "abcd" (as-one-line text2)
    "ab;cd" (as-one-line text2 ";")
    ))

(deftest test-str-contains
  (is (not (str-contains? nil "o")))
  (is (not (str-contains? "foo" nil)))
  (is (not (str-contains? "foo" "g")))
  (is (str-contains? "foo" "o"))
  (is (not (str-contains? "foo" "|"))) )

(deftest test-create-shell-cut
  (is (= "cut -d\";\" -f3,5" (create-shell-cut-with-keys [:c :b :a :e :d] [:a :d] ";")))
  (is (= "cut -d\";\" -f1-3" (create-shell-cut-with-keys [:c :b :e] [] ";")))
  )

(deftest test-regex-group-into-map
  (are [x y] (= x y)
    nil (regex-group-into-map "123" [:a :b :c] #"," [])
    nil (regex-group-into-map "123" [:a :b :c] #"(,)" [])
    {:a "1"} (regex-group-into-map "1,2,3" [:a :b :c] #"(\w)" [])
    {:a "1" :b "2" :c "3"} (regex-group-into-map "1,2,3" [:a :b :c] #"(\d),(\d),(\d)" [])
    {:c "3"} (regex-group-into-map "1,2,3" [:a :b :c] #"(\d),(\d),(\d)" [:c])
    )
  )

(deftest test-regex-groups-into-maps
  (are [x y] (= x y)
    nil (regex-groups-into-maps "a,b,c;d,e,f" [:a :b :c] #"-" [])
    '({:a "a" :b "b" :c "c"}) (regex-groups-into-maps "a,b,c;" [:a :b :c] #"(\w),(\w),(\w)")
    '({:a "a"} {:a "d"}) (regex-groups-into-maps "a,b,c,d,e,f" [:a :b :c] #"(\w),(\w),(\w)" [:a])
    )
  )
