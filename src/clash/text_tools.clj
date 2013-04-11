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
  "Does a String contain a specific string?"
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
        text)) ) )

(defn count-tokens
  "Count the number of tokens in a string (text) for
  a given string delimiter (token)."
  [text, token]
  (if (or (empty? text) (empty? token))
    0
    (count (re-seq (re-pattern token) text))) )