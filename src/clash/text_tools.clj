;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:author "David Millett"
      :doc "Some useful text tools."}
  clash.text_tools
  (:require [clojure.string :as s]))

(defn as-one-line
  "Remove newline characters from a given string and substitue with \"\" (default)
  or some other non empty? delimiter"
  ([text]
    (if (empty? text)
      text
      (s/replace text "\n" "") ) )
  ([text delim]
    (if (or (empty? text) (empty? delim))
      text
      (s/replace text "\n" delim) )) )

(defn str-contains?
  "@deprecated (see clojure 1.8 includes?)
  Does a String contain a specific piece of text?"
  [^String text, ^String search]
  (if (or (empty? text) (empty? search))
    false
    (.contains text search) ) )

(defn count-tokens
  "Count the number of tokens in a string (text) for
  a given string delimiter (token)."
  [^String text ^String token]
  (if (or (empty? text) (empty? token))
    0
    (count (re-seq (re-pattern token) text)) ) )

; Use with clash for best performance
(defn create-shell-cut-with-keys
  "Build a shell 'cut' command with a specific delimiter and specified fields. This
  is more performant than using log-line-to-map to return a 'sub-map' of values"
  [^String structure, keys, ^String delim]
  ; See clojure 1.8 (index-of)
  (let [indices (map #(inc (.indexOf structure %)) keys)
        cut (str "cut -d" \" delim \" " -f")]
    (if (empty? indices)
      (str cut "1-" (count structure))
      (str cut (s/join "," indices)) )) )

(defn text-structure-to-map
  "Split a structured text into map and return some/all entries. A specific
  pattern is required. If specific keys exist, then this functions creates
  a sub-map of the original map."
  ([line pattern structure] (text-structure-to-map line pattern structure []))
  ([line pattern structure keys]
    (when (seq line)
      (let [result (zipmap structure (s/split line pattern))]
        (if (empty? keys)
          result
          (select-keys result keys)) ))) )

(defn regex-group-into-map
  "Parse the first regex match with a specific structure using 're-find' into
  a map. Specify keys to return a subset of the map structure. Empty text or nil
  pattern/structure results in nil."
  ([text structure pattern] (regex-group-into-map text structure pattern []))
  ([text structure pattern sub-keys]
    (when-not (or (empty? text) (nil? pattern) (nil? structure))
      (let [matches (re-find pattern text)]
        (if-not (and (nil? matches) (< (count matches) 1))
          (let [result (zipmap structure (rest matches))]
            (if (empty? sub-keys)
              result
              (select-keys result sub-keys))) ))) ))


(defn regex-groups-into-maps
  "Parse multiple regex matches with structures, using 're-seq', into a map.
   To return a subset of each structure in the sequence, then specify 'sub-keys'.
   Empty text or nil pattern/structure results in nil

   Example: \"a,b,c,d,e,f\" with pattern (\\w),(\\w),(\\w) and structure [:a :b :c]
            will return ({:a a :b b :c c} {:a d :b e :c f})"
  ([text structure pattern] (regex-groups-into-maps text structure pattern []))
  ([text structure pattern sub-keys]
    (when-not (or (empty? text) (empty? structure) (nil? pattern))
       (let [text_groups (re-seq pattern text)]
         (when (seq text_groups)
           (if (empty? sub-keys)
             (map #(zipmap structure (rest %)) text_groups)
             (map #(select-keys (zipmap structure (rest %)) sub-keys) text_groups)) ))) ))

(defn includes-icase?
  "Check if text includes substring regardless of case. Everything
  is converted to lower-case."
  [text substr]
  (if (and text substr)
    (s/includes? (s/lower-case text) (s/lower-case substr))
    false
    ))
