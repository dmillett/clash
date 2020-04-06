;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.shape_test
  (:require [clojure.data.json :as json]
            [clojure.string :as s])
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
        parser2 (fn [line] (try (when (not-empty line) (flatten-json (json/read-str line))) (catch Exception _ (println "Error:" line))))
        freqs1 (transform-lines testfile simple-json-parser :joinfx keypath-frequencies :initv {})
        freqs2 (transform-lines testfile parser2 :joinfx keypath-frequencies :initv {})]

    (is (= {"a" 4, "b" 1, "c" 1, "b.c" 3, "b.d" 2, "b.d.e" 1, "b.d.f" 1} freqs1))
    (is (= freqs1 freqs2))
    ) )

(deftest test-value-patterns
  (are [x y] (= x y)
    :int (value-pattern 1 simple_patterns)
    :decimal (value-pattern 2.0 simple_patterns)
    :financial (value-pattern "USD 0.50" simple_patterns)
    :boolean (value-pattern false simple_patterns)
    :email (value-pattern "foo@zanzibar.zoo" simple_patterns)
    :text (value-pattern "this is a longer sentence" simple_patterns)
    :empty (value-pattern "" simple_patterns)
    ))

(deftest test-transform-lines-for-value-patterns
  (let [input (tresource "data-sample.json")
        parser simple-json-parser
        freqs (transform-lines input parser :joinfx (partial keypath-value-patterns simple_patterns) :initv {})
        ]
    (is (= freqs {"a" {:int 4}, "b" {:decimal 1}, "c" {:int 3}, "b.c" {:financial 3, :int 4}, "b.d" {:int 3, :decimal 1}, "b.d.e" {:int 4}, "b.d.f" {:boolean 1}}))
    ))

(def expected_inclinician
  {"data.descriptions.stomach" {:empty 3}, "data.spectrometer.device" {:text 3}, "date_time" {:decimal 3},
   "data.pulse.value" {:int 3}, "data.oxygen.device" {:text 3}, "data.prescription.dose" {:empty 3},
   "blockchain.contract" {:empty 3}, "age" {:int 3}, "data.descriptions.arms" {:empty 3},
   "data.descriptions.bowels" {:empty 2, :text 1}, "data.descriptions.hands" {:empty 3}, "blockchain.id" {:int 3},
   "data.descriptions.lungs" {:empty 1, :text 2}, "data.urinalysis.device" {:text 3}, "data.pulse.device" {:text 3},
   "data.descriptions.legs" {:empty 3}, "gender" {:empty 3}, "data.descriptions.general" {:empty 1, :text 2},
   "data.oxygen.value" {:int 3}, "data.temperature.device" {:text 3}, "data.breathalyzer.device" {:text 3},
   "data.descriptions.feet" {:empty 3}, "data.stool_analysis.device" {:text 3}, "identity" {:int 3},
   "data.descriptions.skin" {:empty 3}, "data.prescription.time" {:empty 3},
   "data.descriptions.back" {:empty 1, :text 2}, "data.descriptions.throat" {:empty 1, :text 2},
   "data.prescription.form" {:text 3}, "data.ultrasound.location" {:empty 3},
   "data.descriptions.heart" {:empty 2, :text 1}, "data.descriptions.urine" {:empty 3},
   "data.descriptions.head" {:empty 1, :text 2}, "data.descriptions.eyes" {:empty 1, :text 2},
   "location.postal-code" {:empty 3}, "data.saliva_analysis.device" {:text 3}, "data.ultrasound.device" {:text 3},
   "data.immune_signals.device" {:text 3}, "data.immune_signals.location" {:empty 3},
   "data.temperature.value" {:decimal 3}, "data.descriptions.intestines" {:empty 3},
   "data.prescription.name" {:empty 3}, "data.descriptions.sinus" {:empty 1, :text 2},
   "data.spectrometer.location" {:empty 3}, "data.descriptions.energy" {:empty 1, :text 2},
   "data.descriptions.teeth" {:empty 3}, "location.type" {:text 3}, "data.descriptions.tongue" {:empty 3}})

(deftest test-transform-corona-health
  (let [input (tresource "inclinician-data.json")
        parser simple-json-parser
        freq_patterns (transform-lines input parser :joinfx (partial keypath-value-patterns simple_patterns) :initv {})
        ]
    (is (= freq_patterns expected_inclinician))
    ) )

(deftest test-shape-sort
  (let [input (tresource "data-sample.json")
        freqs (transform-lines input simple-json-parser :joinfx (partial keypath-value-patterns simple_patterns) :initv {})
        shaped (shape-sort freqs)
        ]
    (is (= shaped {"b.c" {:int 4, :financial 3}, "b.d.e" {:int 4}, "b.d" {:int 3, :decimal 1}, "a" {:int 4}, "c" {:int 3}, "b.d.f" {:boolean 1}, "b" {:decimal 1}} ))
    ))

(deftest test-xml-json-flattener
  (let [input (tresource "json-xml-sample.txt")
        freqs (transform-lines input xml-and-json-parser :joinfx keypath-frequencies :initv {})
        shaped (shape-sort (transform-lines input xml-and-json-parser :joinfx (partial keypath-value-patterns simple_patterns) :initv {}))]

    (is (= freqs {"A.E.F.@f" 1, "b.d.e" 1, "A.@a" 1, "A.E.F" 1, "b.d" 2, "b.c" 3, "a" 4, "b.d.f" 1, "A.@ax" 1, "A.B.C" 1, "A.D" 1, "b" 1, "A.B.C.@c" 1, "c" 1}))
    (is (= shaped {"b.c" {:int 4, :financial 3}, "b.d.e" {:int 4}, "b.d" {:int 3, :decimal 1}, "a" {:int 4}, "c" {:int 3}, "A.E.F.@f" {:int 2}, "A.B.C.@c" {:int 2}, "A.B.C" {:text 2}, "b.d.f" {:boolean 1}, "b" {:decimal 1}, "A.E.F" {:text 1}, "A.D" {:text 1}, "A.@ax" {:int 1}, "A.@a" {:int 1}}))
    ))