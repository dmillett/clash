;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.regex-test
  (:require [clojure.test :refer :all])
  (:use [clash.regex]))

(deftest test-pattern
  (are [x y] (= x y)
    nil (pattern-rex nil)
    nil (pattern-rex "foo")
    "\\d\\w\\d\\s\\?\\]" (pattern-rex ["\\d" "\\w" "\\d" "\\s" "\\?" "\\]"])
    "\\d\\w\\d\\s\\?\\]" (. (pattern-rex ["\\d" "\\w" "\\d" "\\s" "\\?" "\\]"] false) pattern)
    ) )

(deftest test-map-regex
  (are [x y] (= x y)
    nil (map-regex nil)
    "\\d" (map-regex \0)
    "\\w" (map-regex \b)
    "\\s" (map-regex \space)
    "\\?" (map-regex \?)
    "." (map-regex \-)
    ) )

(deftest test-simplify-regex
  (are [x y] (= x y)
    [] (simplify-regex nil)
    ["\\w{2}"] (simplify-regex ["\\w" "\\w"] :mergex nil)
    ["\\w{2}" "\\d"] (simplify-regex ["\\w" "\\w" "\\d"] :mergex nil)
    ["[\\w\\d]{3}"] (simplify-regex ["\\w" "\\w" "\\d"] :mergex (:basic_alphanum char_literals))
    ) )

(deftest test-build-regex
  (are [x y] (= x y)
    "\\w\\s\\d{2}\\?" (build-regex "A 42?" :mergex nil)
    "\\w\\s\\d.{14}" (build-regex "A 42? Perhaps 67?" :mergex nil :max 3)
    "\\w\\s\\d{2}\\?\\s[\\w\\d]{9}\\?" (build-regex "A 42? Perhaps67?")
    "\\w\\s\\d{2}\\?\\s[\\w\\d]{8}.{2}" (build-regex "A 42? Perhaps67?" :max 14)
    ) )