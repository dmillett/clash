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
  "Works through a file, by line (does not hold head), and creates an 'atom map',
  object store 'ref' available for use in other locations (like an interactive
  repl). Specify a file to open, predicate to include the line, a transformer
  to convert the text, and a structure to parse text into.

  Usage: (atomic-map-from-file \"log-file-foo.txt\"  "
  [input predicate transform parse]
  (let [result (atom {})]
    (with-open [input_reader (reader input)]
      ; Using 'doseq' releases the head of the file to avoid out of memory
      (doseq [line (line-seq input_reader)]
        (if (predicate line)
          (let [tranformed (if-not (nil? transform) (transform line) line)
                structure (parse tranformed)
                key (System/nanoTime)
                current (hash-map key structure)]
            ; Merge the current structure into 'result' bypassing immutability
            (swap! result merge current)) ))) result) )


