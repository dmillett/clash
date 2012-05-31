;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
    ^{:author "David Millett"
      :doc "Some potentially useful tools with command.clj or other."}
  clash.java.tools )

(use '[clojure.java.io :only(reader)])

(defn stb
  "Sh*t the bed message."
  [message]
   (throw (RuntimeException. (str message))) )
  
(defn str-contains?
  "Does a string contain a given text." 
  [text, chars]
  (if-not (or (empty? text) (empty? chars))
    (. text contains chars)) )

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

(defn count-lines
  "How many lines in a small file?"
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq rdr))) )