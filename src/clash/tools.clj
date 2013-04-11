;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
    ;^{:author "David Millett"
    ;  :doc "Some potentially useful tools with command.clj or other."}
  clash.tools
  (:require [clojure.string :as str])
  (:use [clojure.java.io :only (reader)]))

(defn stb
  "Sh*t the bed message."
  [message]
   (throw (RuntimeException. (str message))) )

(defn millis
  "Convert nano seconds to milliseconds."
  [nt]
  (/ nt 1000000.0))

(defn seconds
  "Turn nano seconds into seconds."
  [nt]
  (/ (double nt) 1000000000))

(defn nano-time
  "How many nano seconds from 'start'."
  [start]
  (- (System/nanoTime) start))

(defn microsoft?
  "Is the operating Microsoft based (stb ;-)"
  (. equalsIgnoreCase (System/getProperty "os.name")))

(defn formatf
  "Format a number to scale. Ex: (formatf 1 3) --> 1.000"
  [number scale]
  (format (str "%." scale "f") (double number)) )

(defn elapsed
  "An text message with adjusted execution time (ns, ms, or s)."
  ([time message] (elapsed time message 4))
  ([time message digits]
    (cond
      (< time 99999) (str message " Time(ns):" time)
      (< time 99999999) (str message " Time(ms):" (formatf (millis time) 3))
      :else (str message " Time(s):" (formatf (seconds time) 3)))) )

(defmacro perf
  "Determine function execution time in nano seconds. Display is
  in nanos or millis or seconds (see elapsed())."
  [exe message]
  `(let [time# (System/nanoTime)
         result# ~exe]
     (println (elapsed (nano-time time#) ~message))
     result#))

(defn count-file-lines
  "How many lines in a small file?"
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq rdr))) )

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

