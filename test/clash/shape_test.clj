;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.shape_test
  (:require [clojure.data.json :as json])
  (:use [clojure.test]
        [clash.shape]
        [clash.core]))

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
  (let [freqs (flat-data-value-counts (flatten-xml xml1))]
    (are [x y] (= x y)
      1 (get freqs "A.@ax")
      1 (get freqs "A.@a")
      2 (get freqs "A.B.C.@c")
      2 (get freqs "A.B.C")
      1 (get freqs "A.D")
      2 (get freqs "A.E.F.@f")
      1 (get freqs "A.E.F")
      ) ) )

(def json1 "{\"a\":1, \"b\":2, \"c\":[3,4,5]}")
(def json2 "{\"a\":1, \"b\":{\"c\":3, \"d\":4}}")
(def json3 "{\"a\":1, \"b\":[{\"c\":2, \"d\":3},{\"c\":4, \"d\":5},{\"c\":6, \"d\":7}]}")
(def json4 "{\"a\":1, \"b\":[{\"c\":2, \"d\":{\"e\":[8,9]}},{\"c\":4, \"d\":{\"e\":[10,11]}},{\"c\":6, \"d\":{\"f\":true}}]}")

(deftest test-flatten-json
  (let [d1 (flatten-json (json/read-str json1))
        d2 (flatten-json (json/read-str json2))
        d3 (flatten-json (json/read-str json3))
        d4 (flatten-json (json/read-str json4))]

    (are [x y] (= x y)
      {"a" [1] "b" [2] "c" [3 4 5]} d1
      {"a" [1], "b.c" [3], "b.d" [4]} d2
      {"a" [1], "b.c" [2 4 6], "b.d" [3 5 7]} d3
      {"a" [1], "b.c" [2 4 6], "b.d.e" [8 9 10 11], "b.d.f" [true]} d4
      {"a" [1 1 1 1], "b" [2], "c" [3 4 5], "b.c" [3 2 4 6 2 4 6], "b.d" [4 3 5 7], "b.d.e" [8 9 10 11], "b.d.f" [true]} (merge-data d1 d2 d3 d4)
      ) ) )

(defn tresource
  "Define the current test directory."
  [f]
  (str (System/getProperty "user.dir") "/test/resources/" f))

(deftest test-transform-lines-for-keypaths
  (let [testfile (tresource "data-sample.json")
        parser (fn [line] (flatten-json (json/read-str line)))
        keypath_freqs (transform-lines testfile parser :joinfx keypath-frequencies :initv {})]
    (is (= {"a" 4, "b" 1, "c" 1, "b.c" 3, "b.d" 2, "b.d.e" 1, "b.d.f" 1} keypath_freqs))
    ) )