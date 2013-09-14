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
  clash.tools
  (:require [clojure.string :as str])
  (:use [clojure.java.io :only (reader)])
  (:import [java.text.SimpleDateFormat]
           [java.text SimpleDateFormat])
  )

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
  "Is the operating Microsoft based ('win')"
  []
  (= "win" (System/getProperty "os.name")))

(defn formatf
  "Format a number to scale. Ex: (formatf 1 3) --> 1.000"
  [number scale]
  (format (str "%." scale "f") (double number)) )

(defn elapsed
  "An text message with adjusted execution time (ns, ms, or s)."
  ([time] (elapsed time "" 4))
  ([time message] (elapsed time message 4))
  ([time message digits]
    (cond
      (< time 99999) (str message " Time(ns):" time)
      (< time 99999999) (str message " Time(ms):" (formatf (millis time) 3))
      :else (str message " Time(s):" (formatf (seconds time) 3)))) )

(defn format-nanotime-to
  "Format nano time (9 digits) to a specified date-time format
  uses java SimpleDateFormat."
  [nano_time date_fmt]
  (.. (SimpleDateFormat. date_fmt) (format nano_time)) )

;; todo: convert HH mm DD to nanotime pattern

(defmacro latentcy
  "A macro to determine the latency for function execution. Returns a map
  with ':latency', ':latency_text', and ':result'"
  [exe]
  `(let [time# (System/nanoTime)
         result# ~exe
         latentcy# (nano-time time#)
         latentcy_text# (elapsed latentcy#)]
     {:latency_text latentcy_text# :latentcy latentcy# :result result#} ) )

(defmacro perf
  "Determine function execution time in nano seconds. Display is
  in nanos or millis or seconds (see elapsed()). Println time side
  effect. This was first macro I wrote and is functionally equivalent
  to 'latency' function."
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


