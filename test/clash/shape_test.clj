;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.shape_test
  (:use [clojure.test]
        [clash.shape]))

(def xml1 "<A a=\"a1\" ax=\"ax1\"><B><C c=\"c1\"/><C c=\"c2\">foo</C><C>bar</C></B><D>zoo</D><E><F f=\"f1\">cats</F><F f=\"f1\"/></E></A>")

(deftest test-flatten-xml
  (let [flattened (flatten-xml xml1)]
    (are [x y] (= x y)
       ["ax1"] (get flattened "A.@ax")
       ["a1"] (get flattened "A.@a")
       ["c1" "c2"] (get flattened "A.B.C.@c")
       ["foo" "bar"] (get flattened "A.B.C")
       ["zoo"] (get flattened "A.D")
       ["f1" "f1"] (get flattened "A.E.F.@f")
       ["cats"] (get flattened "A.E.F")
       ) ) )

(deftest test-flat-data-frequencies
  (let [freqs (flat-data-frequencies (flatten-xml xml1))]
    (are [x y] (= x y)
      1 (get freqs "A.@ax")
      1 (get freqs "A.@a")
      2 (get freqs "A.B.C.@c")
      2 (get freqs "A.B.C")
      1 (get freqs "A.D")
      2 (get freqs "A.E.F.@f")
      1 (get freqs "A.E.F")
      ) ) )