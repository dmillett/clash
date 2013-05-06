;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
  ^{:author "David Millett"
    :doc "Atomizing and interacting with oject memory stores from text files (logs, etc)."}
  clash.interact
  (:use [clojure.java.io :only (reader writer)])
  )

;;
;; For a pure map/list structure with no defrecords (~10% slower)
;; (let [decoded (tt/url-decode line)] (sd/parse-soldump-line decoded))
;;   (sd/parse-soldump-line line)
(defn atomic-map-from-file
  "Load ~structured text from a file into a map of data structures to interact with
  at the command line (repl). Larger files and structures may require increasing the
   jvm heap. It helps to have specific regex to decrease the number of 'bad'
   structures included in the atomized data structure.

  Usage:
  (atomic-map-from-file \"/foo.log\" foo-parser)
  (atomic-map-from-file \"/foo.log\" is-foo? url-decode foo-parser key-builder)

  'input' - a text file with structure (typically a log file)
  'parser' - function that parses a text line, with regex, into a data structure
  'predicate' - function includes/excludes text line from parsing
              - default allows every line
              - define function using 'every-pred' for 1 - N predicates
  'transformer' - function to alter text line (decode, decrypt, etc) prior to parsing
                - defaults to no transformation
  'key' - function to generate a unique key for the map
        - can use structured object to generate key
        - defaults to (System/nanoTime)

   Alternatively, just the 2 arg function and rely on the parser to perform much of
   the functionality with more strict regex."
  ([input parser] (atomic-map-from-file input nil nil parser nil))
  ([input predicate transformer parser key]
    (let [result (atom {})]
      (with-open [input_reader (reader input)]
        ; Using 'doseq' releases the head of the file to avoid out of memory
        (doseq [line (line-seq input_reader)]
          (if (or (nil? predicate) (predicate line))
            (let [tranformed (if-not (nil? transformer) (transformer line) line)
                  structure (parser tranformed)
                  k (if-not (nil? key) (key structure) (System/nanoTime))
                  current (hash-map k structure)]
              ; Merge the current structure into 'result' bypassing immutability
              (if-not (nil? current) (swap! result merge current)) ) ))) result)) )


(defn atomic-list-from-file
  "Load ~structured text from a file into a list of data structures to interact with
  at the command line (repl). Larger files and structures may require increasing the
   jvm heap. It helps to have specific regex to decrease the number of 'bad'
   structures included in the atomized data structure.

  Usage:
  (atomic-list-from-file \"/foo.log\" foo-parser)
  (atomic-list-from-file \"/foo.log\" is-foo? url-decode foo-parser)

  'input' - a text file with structure (typically a log file)
  'parser' - function that parses a text line, with regex, into a data structure
  'predicates' - function includes/excludes text line from parsing
               - default will allow every line
               - define predicates with 'every-pred' for 1 - N predicates
  'transformer' - function to alter text line (decode, decrypt, etc) prior to parsing
                - defaults to no text transformation

   Alternatively, just the 2 arg function and rely on the parser to perform much of
   the functionality with more strict regex."
  ([input parser] (atomic-list-from-file input nil nil parser))
  ([input predicate transformer parser]
    (let [result (atom '())]
      (with-open [input_reader (reader input)]
        (doseq [line (line-seq input_reader)]
          (if (or (nil? predicate) (predicate line))
            (let [transformed (if-not (nil? transformer) (transformer line) line)
                  data (parser transformed)]
              ; Updating 'result' with 'data'
              (if-not (nil? data) (swap! result conj data)) )) )) result) ) )

;;
;; To pass along more than one condition, use (every-pred p1 p2 p3)
;; Example: (def even3 (every-pred even? #(mod % 3)))
(defn count-with-conditions
  "Perform a count on each data structure in a list if it matches
  the conditions defined in the predicates function. The predicates
  function may contain multiple conditions when used with (every-pred p1 p2)."
  [solutions predicates]
  (reduce (fn [count solution]
            (if (or (nil? predicates) (predicates solution))
              (inc count)
              count
              ))
    0 solutions) )

(defn collect-with-condition
  "Build a collection of structued data objects that satisfy the conditions
  defined in 'predicates'. The predicates should be customized to use the
  data structure to filter."
  [solutions predicates]
  (if (nil? predicates)
    solutions
    (filter #(predicates  %) solutions) ) )