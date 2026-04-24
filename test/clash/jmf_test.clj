;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.jmf-test
  (:require [clash.text_tools :as ctt]
            [clojure.test :refer :all]
            [clojure.string :as cstr]
            [clash.core :as cc]
            [clash.shape :as cs]
            [clash.regex :as crx])
  (:use [clash.jmf]))

(defn test-resource
  "Get a test resource file."
  [f]
  (str (System/getProperty "user.dir") "/test/resources/" f))

(def reg1
  {"a" {"\\d" 4},
   "b" {"\\d\\.\\d" 1},
   "c" {"\\d" 3},
   "b.c" {"\\w{3}\\s\\d{2}\\.\\d{2}" 2, "\\w{3}\\s\\d\\.\\d{2}" 1, "\\d" 4},
   "b.d" {"\\d" 3, "\\d\\.\\d" 1},
   "b.d.e" {"\\d" 2, "\\d{2}" 2},
   "b.d.f" {"\\w{4}" 1}})

(def reg2
  {"a"
   {"\\w{3}\\_\\w{4}.{1}" 2, "\\w{5}" 1},
   "b" {"\\w{3}\\s\\w{4}.{7}" 1, "\\w{6}\\s\\w.{19}" 1, "\\w{3}\\s\\w{4}.{28}" 1},
   "c" {"\\w" 9}}
  )

(deftest test-jmf-value-regex
  (let [input1 (test-resource "data-sample.json")
        jmf1 (jmf-value-regex crx/build-regex)
        input2 (test-resource "data2.json")
        jmf2 (jmf-value-regex #(crx/build-regex % :max 8))
        ]
    (are [x y] (= x y)
      reg1 (cc/transform-lines input1 cs/xml-and-json-parser :initv {} :joinfx jmf1)
      reg2 (cc/transform-lines input2 cs/xml-and-json-parser :initv {} :joinfx jmf2)
      ) ) )

(def word-freqs1
  {"who" 1, "this" 1, "maybe" 1, "etc" 1, "it" 1, "is" 2, "about" 1, "just" 1, "words" 1,
   "slightly" 1, "another" 1, "nothing" 1, "special" 2, "knows" 1, "numbers" 1, "with" 1,
   "file" 1, "well" 1, "data" 1, "there" 1})

(deftest test-jmf-word-count
  (let [input1 (test-resource "data4.txt")]
    (are [x y] (= x y)
      word-freqs1 (cc/transform-lines input1 identity :initv {} :joinfx (jmf-word-count))
      ) ) )

(deftest test-jmf-kv-filter
  (let [input1 (test-resource "data-sample.json")
        kfilter #(contains? #{"b.c" "b.d.f"} %)
        vfilter #(cstr/includes? % "EUR")
        k1 (cc/transform-lines input1 cs/xml-and-json-parser :initv {} :joinfx (jmf-merge-kv-filter kfilter nil))
        k2 (cc/transform-lines input1 cs/xml-and-json-parser :initv {} :joinfx (jmf-merge-kv-filter nil vfilter))
        k3 (cc/transform-lines input1 cs/xml-and-json-parser :initv {} :joinfx (jmf-merge-kv-filter nil nil))
        ]
    (are [x y] (= x y)
      2 (count k1)
      1 (count k2)
      0 (count k3)
      ) ) )

(deftest test-jmf-kv-regex-count
  (let [input1 (test-resource "data-sample.json")
        vr1 (cc/transform-lines input1 cs/xml-and-json-parser :initv {} :joinfx (jmf-kv-regex-count ["b.c" "b.d.f"] ctt/word-count))
        ]
    (are [x y] (= x y)
      {"b.c" {"EUR" 1} "b.d.f" {"true" 1}} vr1
      ) ) )