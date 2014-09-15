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
  clash.core
  (:require [clash.tools :as t])
  (:use [clojure.java.io :only (reader writer)]) )

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
  (fn [item]
    (loop [result true
            preds predicates]
       (if (or (not result) (empty? preds))
         result
         (recur ((first preds) item) (rest preds))
         ) ) ) )

(defn any?
  "Pass value(s) implicitly and a list of predicates explicitly for evaluation.
  If any predicate returns 'true', then function returns 'true'. Otherwise
  function returns 'false'. Ex: ((any? number? odd?) 10) --> true"
  [& predicates]
  (fn [item]
    (loop [result false
            preds predicates]
       (if (or result (empty? preds))
         result
         (recur ((first preds) item) (rest preds))
         ) ) ) )

(defn until?
  "Returns 'true' for the first item in a collection that satisfies the predicate.
  Otherwise returns 'false'"
  [pred coll]
  (loop [result false
         items coll]
    (cond
      (empty? items) result
      (try (pred (first items)) (catch Exception e false)) true
      :else (recur result (rest items))
      ) ) )

(defn take-until
  "A compliment to (take-while). Gather values of a collection into a list until
  the predicate is satisfied. Otherwise returns an empty list."
  [pred coll]
  (loop [result '()
         items coll]
    (cond
      (and (empty? items) (= (count coll) (count result))) '()
      (empty? items) result
      (try (pred (first items)) (catch Exception e false)) (conj result (first items))
      :else (recur (conj result (first items)) (rest items))
      ) ) )

(defn- transform-text
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

(defn file-into-structure
  "Load ~structured text from a file into a data structure to interact with
  at the command line (repl). Larger files and structures may require increasing the
   jvm heap. It helps to have specific regex to decrease the number of 'bad'
   structures included in the atomized data structure.

  Usage:
  (file-into-structure \"/foo.log\" foo-parser [])    ;; Attempt to load all
  (file-into-structure \"foo.log\" foo-parser [] 50)  ;; Load first 50
  (file-into-structure \"/foo.log\" is-foo? url-decode foo-parser [] -1) ;; Attempt to load all

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
  ([input parser structure] (file-into-structure input nil nil parser structure -1))
  ([input parser structure max] (file-into-structure input nil nil parser structure max))
  ([input predicate transformer parser structure max]
    (into structure (deref (atomic-list-from-file input predicate transformer parser max)))
    ) )


;;
;; To pass along more than one condition, use (every-pred p1 p2 p3)
;; Example: (def even3 (every-pred even? #(mod % 3)))
(defn count-with
  "Perform a count on each data structure in a list if it matches
  the conditions defined in the predicates function. The predicates
  function may contain multiple conditions when used with (every-pred p1 p2)."
  ([solutions predicates] (count-with solutions predicates nil 0))
  ([solutions predicates initial] (count-with solutions predicates nil 0))
  ([solutions predicates incrementer initial]
    (reduce (fn [count solution]
              (if (or (nil? predicates) (predicates solution))
                (if-not (nil? incrementer) (incrementer solution count) (inc count))
                count
                ))
      initial solutions) ))

(defn calculate-with
  "Perform a count on each data structure in a list if it matches
  the conditions defined in the predicates function. The predicates
  function may contain multiple conditions when used with (every-pred p1 p2)."
  ([solutions predicates] (calculate-with solutions predicates nil 0))
  ([solutions predicates initial] (calculate-with solutions predicates nil 0))
  ([solutions predicates incrementer initial]
    (reduce (fn [count solution]
              (if (or (nil? predicates) (predicates solution))
                (if-not (nil? incrementer) (incrementer solution count) (inc count))
                count
                ))
      initial solutions) ))

(defn collect-with
  "Build a collection of structued data objects that satisfy the conditions
  defined in 'predicates'. The predicates should be customized to use the
  data structure to filter."
  [solutions predicates]
  (if (nil? predicates)
    solutions
    (filter (fn [sol] (predicates  sol)) solutions) ) )

(defn- generate-pivot-functions
  "Create a list of functions given a list of values and add
  meta-data to them with {:name 'pivot-by-'} "
  [pivot_f values]
  (map #(with-meta (pivot_f %) {:name (str "pivot-by-" %)}) values) )

(defn- combine-functions-with-meta
  "Carry the metadata :name forward from the pivot functions"
  [f preds metafs]
  ; copy meta data from pivot functions when appending them to predicates
  (map #(with-meta
          (apply f (conj preds %))
          {:name (:name (meta %))}) metafs) )

(defn pivot
  "Evaluate each value in a collection (col) with a base set of predicates (preds)
  and a 'pivot' predicate with its list of corresponding pivot values. This function
   returns a map sorted descending by pivot count. By default, (pivot) will use
  the conditional all? (and), but any? (or) could also be used. For example:

  ; 6 is an even number dividable by 2, 3
  ; 8 is an even number dividable by 2
  ; 7 is an odd number (it does not satisfy any of the composite predicates)
  user=> (pivot '(6 7 8) [number? even?] divisible-by? '(2 3))

  {pivot-by-2 2, pivot-by-3 1}
  "
  ([col preds pivotf pivotd] (pivot col all? preds pivotf pivotd))
  ([col f preds pivotf pivotd]
    (let [fpivots (generate-pivot-functions pivotf pivotd)
          combos (combine-functions-with-meta f preds fpivots)]

      (t/sort-map-by-value
        (reduce
          (fn [r f]
            (assoc-in r [(:name (meta f))] (count-with col f)) )
          {} combos) )
      ) ) )