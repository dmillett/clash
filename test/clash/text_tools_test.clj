;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.text_tools_test)
  (:use ;[clojure.test]
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

