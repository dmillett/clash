;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
    ;^{:author "David Millett"
    ;  :doc "Some useful text tools."}
  clash.text_tools
  (:require [clojure.string :as str]))

(defn as-one-line
  "Remove newline characters from a given string and substitue with \"\" (default)
  or some other non empty? delimiter"
  ([text]
    (if (empty? text)
      text
      (str/replace text "\n" "") ) )
  ([text delim]
    (if (or (empty? text) (empty? delim))
      text
      (str/replace text "\n" delim) )) )

(defn str-contains?
  "Does a String contain a specific piece of text?"
  [text, search]
  (if (or (empty? text) (empty? search))
    false
    (.contains text search) ) )

(defn split-with-regex
  "Split a message with a given character or regex"
  [text, regex]
  (if-not (or (nil? text) (nil? regex))
    (str/split text (re-pattern regex)) ) )

(defn replace-delim
  "Replace a 'delim1' in 'text' with 'delim2'. Otherwise
  return 'text' if there are no matching delim1 values.
  Ex:  (replace-delim a_b_c _ :) --> a:b:c "
  [text, d1, d2]
  (if (or (empty? text) (empty? d1) (empty? d2))
    text
    (let [result (str/split text (re-pattern d1))
          number (count result)]
      (if (> number 1)
        (apply str (interpose d2 result))
        text) ) ) )

(defn count-tokens
  "Count the number of tokens in a string (text) for
  a given string delimiter (token)."
  [text, token]
  (if (or (empty? text) (empty? token))
    0
    (count (re-seq (re-pattern token) text)) ) )

; Use with clash for best performance
(defn create-shell-cut-with-keys
  "Build a shell 'cut' command with a specific delimiter and specified fields. This
  is more performant than using log-line-to-map to return a 'sub-map' of values"
  [structure keys delim]
  (let [indices (map #(+ 1 (.indexOf structure %)) keys)
        cut (str "cut -d" \" delim \" " -f")]
    (if (empty? indices)
      (str cut "1-" (count structure))
      (str cut (apply str (interpose \, indices))) )) )

(defn text-structure-to-map
  "Split a structured text into map and return some/all entries. A specific
  pattern is required. If specific keys exist, then this functions creates
  a sub-map of the original map."
  ([line pattern structure] (text-structure-to-map line pattern structure []))
  ([line pattern structure keys]
    (when-not (empty? line)
      (let [result (zipmap structure (str/split line pattern))]
        (if (empty? keys)
          result
          (select-keys result keys)) ))) )

(defn regex-group-into-map
  "Parse the first regex match with a specific structure using 're-find' into
  a map. Specify keys to return a subset of the map structure. Empty text or nil
  pattern/structure results in nil."
  ([text structure pattern] (regex-group-into-map text structure pattern []))
  ([text structure pattern sub-keys]
    (if (or (empty? text) (nil? pattern) (nil? structure))
      nil ; breakout
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
    (if (or (empty? text) (empty? structure) (nil? pattern))
       nil
       (let [text_groups (re-seq pattern text)]
         (when-not (empty? text_groups)
           (if (empty? sub-keys)
             (map #(zipmap structure (rest %)) text_groups)
             (map #(select-keys (zipmap structure (rest %)) sub-keys) text_groups)) ))) ))
