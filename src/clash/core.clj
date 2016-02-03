;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
  ^{:author "dmillett"
    :doc "Atomizing and interacting with oject memory stores from text files (logs, etc)."}
  clash.core
  (:use [clojure.java.io :only (reader writer)]) )

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
  ([input parser] (atomic-map-from-file input nil nil parser nil nil))
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
          (catch OutOfMemoryError _ (println "Insufficient Memory: " (count @result) "Solutions Loaded"))
          (catch Exception e (println "Exception:" e ", " (count @result) " Solutions Loaded"))) )
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
        (catch OutOfMemoryError _ (println "Insufficient Memory: " (count @result) " Solutions Loaded"))
        (catch Exception e (println "Exception:" e ", " (count @result) " Solutions Loaded")))
      result) ) )

(defn transform-lines
  "Transform the text with a transducer instead of doseq. In this case, default
  behavior will 1) parse lines and 2) filter non-nil results into a collection.
  It is possible to specify the max number of good results or to specify a specific
  transducing xform function. For example, get the first 10 lines:

  (transform-lines \"some-file.txt\" some-parser :max 10 :tdfx (filter identity))

  If errors are encountered, try (transform-lines-verbose) or (atomic-list-from-file)
  "
  [input parser & {:keys [max tdfx] :or {max nil tdfx nil}}]
  (let [transducefxA (if tdfx tdfx (comp (map parser) (filter identity)))
        transducefx (if max (comp transducefxA (take max)) transducefxA)]
    (try
      (with-open [ireader (reader input)]
        (transduce transducefx conj [] (line-seq ireader)) )
      (catch Exception e (println "Exception:" (.getMessage e))))
    ) )

(defn transform-lines-verbose
  "Transform the text with a reducer into a vector instead of
  (doseq) into an atom. This will attempt to count every line,
  parse every line, and store the results. It produces this:

  {:c 1  ; the count of lines processed
   :p [] ; the transformed lines
   :f [] ; the lines that failed to transform
  }"
  [input parser & {:keys [max] :or {max nil}}]
  (let [result {:c 0 :p [] :f []}
        rdfx (fn [m line] (if-let [parsed (parser line)]
                         (assoc (assoc m :p (conj (:p m) parsed)) :c (inc (:c m)))
                         (assoc (assoc m :f (conj (:f m) line)) :c (inc (:c m)))
                         ))]
    (try
      (with-open [ireader (reader input)]
        (if max
          (reduce rdfx result (take max (line-seq ireader)))
          (reduce rdfx result (line-seq ireader))
          ) )
      (catch Exception e (println "Exception:" (.getMessage e))))
  ) )

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
