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

;; Calling (every? over every predicate evaluation seems to work with primitives, but not objects
(defn all-preds?
  "Pass value(s) to a list of predicates for evaluation. If all predicates return 'true',
   then function returns 'true'. Otherwise function returns 'false'. Could not pass a function
   list to (every-pred) successfully."
  [values & predicates]
  (loop [result true
         preds predicates]
    (if (or (not result) (empty? preds))
      result
      (recur ((first preds) values) (rest preds) )
      ) ) )

(defn any-preds?
  "Pass value(s) and a list of predicates for evaluation. If any predicate returns 'true',
  then function returns 'true'. Otherwise function returns 'false'."
  [values & predicates]
  (loop [result false
         preds predicates]
    (if (or result (empty? preds))
      result
      (recur ((first preds) values) (rest preds))
      ) ) )

(defn all?
  "Pass value(s) implicitly and a list of predicates explicitly for evaluation.
  If all predicates return 'true', then function returns 'true'. Otherwise
  function returns 'false'. Could not pass a function list to (every-pred)
  successfully. Ex: ((all? number? odd?) 10) --> false"
  [& predicates]
  #(loop [result true
          preds predicates]
     (if (or (not result) (empty? preds))
       result
       (recur ((first preds) %) (rest preds))
       ) ) )

(defn any?
  "Pass value(s) implicitly and a list of predicates explicitly for evaluation.
  If any predicate returns 'true', then function returns 'true'. Otherwise
  function returns 'false'. Ex: ((any? number? odd?) 10) --> true"
  [& predicates]
  #(loop [result false
          preds predicates]
     (if (or result (empty? preds))
       result
       (recur ((first preds) %) (rest preds))
       ) ) )

(defn transform-text
  "A function to transform text from form to another. Example, decode/encode
  URL, decode/encode encryption, etc"
  [transformer ^String text]
  (if-not (nil? transformer) (transformer text) text) )

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

  'max' - max number of solutions/entries to load.

   Alternatively, just the 2 arg function and rely on the parser to perform much of
   the functionality with more strict regex."
  ([input parser] (atomic-map-from-file input nil nil parser nil))
  ([input parser max] (atomic-map-from-file input nil nil parser nil max))
  ([input predicate transformer parser key max]
    (let [result (atom {})]
      (with-open [input_reader (reader input)]
        (try
          (doseq [line (line-seq input_reader)
                  :when (and (or (= -1 max) (> max (count @result)))
                          (or (nil? predicate) (predicate line)))
                  :let [transformed (transform-text transformer line)
                        structure (parser transformed)
                        k (if-not (nil? key) (key structure) (System/nanoTime))
                        current (hash-map k structure)]]
            (if-not (nil? current)
              (swap! result merge current)) )
          (catch OutOfMemoryError e (println "Insufficient Memory: " (count @result) "Solutions Loaded")) ) )
      result) ) )


(defn atomic-list-from-file
  "Load ~structured text from a file into a list of data structures to interact with
  at the command line (repl). Larger files and structures may require increasing the
   jvm heap. It helps to have specific regex to decrease the number of 'bad'
   structures included in the atomized data structure.

  Usage:
  (atomic-list-from-file \"/foo.log\" foo-parser)    ;; Attempt to load all
  (atomic-list-from-file \"foo.log\" foo-parser 50)  ;; Load first 50
  (atomic-list-from-file \"/foo.log\" is-foo? url-decode foo-parser -1) ;; Attempt to load all

  'input' - a text file with structure (typically a log file)
  'parser' - function that parses a text line, with regex, into a data structure
  'predicates'  - function includes/excludes text line from parsing
                - default will allow every line
                - define predicates with 'every-pred' for 1 - N predicates
  'transformer' - function to alter text line (decode, decrypt, etc) prior to parsing
                - defaults to no text transformation
  'max'         - Max number of solutions to load (possibly due to memory constraint)

   Alternatively, just the 2 arg function and rely on the parser to perform much of
   the functionality with more strict regex."
  ([input parser] (atomic-list-from-file input nil nil parser -1))
  ([input parser max] (atomic-list-from-file input nil nil parser max))
  ([input predicate transformer parser max]
    (let [result (atom '())]
      (try
        (with-open [input_reader (reader input)]
          (doseq [line (line-seq input_reader)
                  :when (and (or (= -1 max) (> max (count @result)))
                          (or (nil? predicate) (predicate line)))
                  :let [transformed (transform-text transformer line)
                        data (parser transformed)]]
            (if-not (nil? data)
              (swap! result conj data)) ))
        (catch OutOfMemoryError e (println "Insufficient Memory: " (count @result) " Solutions Loaded")))
      result) ) )

;;
;; To pass along more than one condition, use (every-pred p1 p2 p3)
;; Example: (def even3 (every-pred even? #(mod % 3)))
(defn count-with-conditions
  "Perform a count on each data structure in a list if it matches
  the conditions defined in the predicates function. The predicates
  function may contain multiple conditions when used with (every-pred p1 p2)."
  ([solutions predicates] (count-with-conditions solutions predicates nil 0))
  ([solutions predicates initial_count] (count-with-conditions solutions predicates nil 0))
  ([solutions predicates incrementer initial_count]
    (reduce (fn [count solution]
              (if (or (nil? predicates) (predicates solution))
                (if-not (nil? incrementer) (incrementer solution count) (inc count))
                count
                ))
      initial_count solutions) ))

(defn collect-with-conditions
  "Build a collection of structued data objects that satisfy the conditions
  defined in 'predicates'. The predicates should be customized to use the
  data structure to filter."
  [solutions predicates]
  (if (nil? predicates)
    solutions
    (filter #(predicates  %) solutions) ) )
