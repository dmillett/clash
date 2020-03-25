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


(defn atomic-list-from-file
  "DEPRECATED use (transform-lines)

  Load ~structured text from a file into a list of data structures to interact with
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
            (when data
              (swap! result conj data)) ))
        (catch OutOfMemoryError _ (println "Insufficient Memory: " (count @result) " Solutions Loaded"))
        (catch Exception e (println "Exception:" e ", " (count @result) " Solutions Loaded")))
      result) ) )

(defn file-into-structure
  "DEPRECATED use (transform-lines)

  Load ~structured text from a file into a data structure to interact with
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

(defn transform-lines
  "Transform the text with a transducer instead of doseq. In this case, default
  behavior will 1) parse lines and 2) filter non-nil results into a collection.
  It is possible to specify the max number of good results or to specify a specific
  transducing xform function. For example, get the first 10 lines:

  consumes:
  'input' - An input stream handled with clojure.io.reader
  'parser' - Transforms & maps to a defined structure (or just transforms things like JSON, XML, etc)
  'tdfx' - A transducing function that transforms/maps data (default: (-> identity parser), ignores parser
  'max' - The maximum number of rows to transform
  'joinfx' - How to combine data in transduce, default (conj)
  'initv' - Initial value for transducer, default []

  produces:

  example:
  (transform-lines \"some-file.txt\" some-parser :max 10 :tdfx (filter identity))

  If errors are encountered, try (transform-lines-verbose) or (atomic-list-from-file)
  "
  [input parser & {:keys [max tdfx joinfx initv] :or {max nil tdfx nil joinfx conj initv []}}]
  (let [transducefx (or tdfx (comp (map parser) (filter identity)))]
    (try
      (with-open [ireader (reader input)]
        (if max
          (transduce transducefx joinfx initv (take max (line-seq ireader)))
          (transduce transducefx joinfx initv (line-seq ireader))
          ) )
      (catch Exception e (println "Exception:" (.getMessage e))))
    ) )

(defn transform-lines-verbose
  "Transform the text with a reducer into a vector instead of (doseq) into an atom. This will
  attempt to count every line, parse every line, and store the results. It produces this:

  consumes:
  'input' - An input stream handled with clojure.io.reader
  'parser' - Transforms & maps to a defined structure (or just transforms things like JSON, XML, etc)
  'max' - The maximum number of rows to transform

  produces:
  {:c 1  ; the count of lines processed
   :p [] ; the transformed lines
   :f [] ; the lines that failed to transform
  }

  It seems that local variables in the (let) assignment improve performance a little bit."
  [input parser & {:keys [max] :or {max nil}}]
  (let [result {:c 0 :p [] :f []}
        rdfx (fn [m line]
               (let [i (inc (:c m))
                     pd (:p m)
                     fd (:f m)]
                 (if-let [parsed (parser line)]
                   (assoc (assoc m :p (conj pd parsed)) :c i)
                   (assoc (assoc m :f (conj fd line)) :c i)
                   ) ) )]
    (try
      (with-open [ireader (reader input)]
        (if max
          (reduce rdfx result (take max (line-seq ireader)))
          (reduce rdfx result (line-seq ireader))
          ) )
      (catch Exception e (println "Exception:" (.getMessage e))))
  ) )

(defn- twriter
  "A writer for transducers."
  [owrite delim]
  (fn
    ([])
    ([result])
    ([result current] (.write owrite (str current delim))) ) )

(defn disect
  "Transform input (stream) to output (stream) with a transducer function. This can be used for 'cleaning'
  input or files before parsing them into memory. For example, parsing large JSON/XML schema just to extract a subset is
   much slower than converting a subset of that data extracted via regular expression(s).

   consumes:
   'input' - An input stream (clojure.io.reader) with data to process/clean handled
   'output' - An output stream (clojure.io.writer) writes the processed data with a delimiter
   'delim' - Which delimiter to separate transformed data for 'output' (defaults to '\n')
   'max' - The maximum number of data to clean via rows from line-seq reader (defaults to -1 = all)
   'fx' - The final step in a transducer flow that manipulates/transforms data (-> identity fx)
   'tdfx' - Specify a transducer function to use instead to transform data

   produces:
   A transformed output stream via clojure.io.writer (file, stream, etc)

   This is usually much faster than 'awk' for regex groups and 'jq'. Although 'jq' is also faster than 'awk'.
   "
  [input output & {:keys [max fx tdfx delim] :or {max -1 fx identity tdfx nil delim "\n"}}]
  (let [transfx (or tdfx (comp (map fx) (filter identity)))]
    (try
      (with-open [iread (reader input)
                  owrite (writer output :append true)]
        (if (pos? max)
          (transduce transfx (twriter owrite delim) (take max (line-seq iread)))
          (transduce transfx (twriter owrite delim) (line-seq iread))
          ) )
      (catch Exception e (println "transform exception:" (.getMessage e))))
    ) )