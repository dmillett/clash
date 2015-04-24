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
  (:require [clojure.string :as str]
            [clojure.core.reducers :as r])
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

(defmacro latency
  "A macro to determine the latency for function execution. Returns a map
  with ':latency', ':latency_text', and ':result'"
  [exe]
  `(let [time# (System/nanoTime)
         result# ~exe
         nano_latency# (nano-time time#)
         latency_text# (elapsed nano_latency#)]
     {:latency_text latency_text# :latentcy nano_latency# :result result#} ) )

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

(defn sort-map-by-value
  "Sort by values descending (works when there are non-unique values too)."
  [m]
  (into (sorted-map-by
          (fn [k1 k2] (compare [(get m k2) k2]
                               [(get m k1) k1]) ) )
    m) )

(defn fold-conj
  "Acts like (conj) but intended for reducers/fold and zero arity."
  ([] '())
  ([a b] (conj a b)) )

(defn compare-map-with
  "Compare values in two maps with a specific 2 arg function. Currently this assumes
  identical keysets in each map. todo: fix for missing keys (set default value)"
  [m1 m2 f]
  (reduce
    (fn [result [k v]]
      ; Should compare when the value is not nil
      (assoc-in result [k] (when (not (nil? v)) (f v (-> k m2))) ) )
    {} m1) )

(defn value-frequencies
  "Find out the frequency of values for each key for a map. This can be useful when
  evaluating a collection of maps/defrecords. Specifying a :kpath will retrieve a map
  at depth, while :kset will limit the result to specific keys. For example:
  (value-frequencies {:a a1}) => {:a {a1 1}}
  (value-frequencies {} {:a 1 :b {:c1 1}} :kpath [:b]) => {:c {c1 1}}"
  ([m] (value-frequencies {} m))
  ([target_map m & {:keys [kset kpath] :or {kset [] kpath []}}]
    (let [kpmap (if (empty? kpath) m (get-in m kpath))
          mp (if (map? kpmap) kpmap {})
          submap (if (empty? kset) mp (select-keys mp kset))]
      (reduce
        (fn [result [k v]]
          (if (or (nil? (get-in result [k])) (nil? (get-in result [k v])))
            (assoc-in result [k v] 1)
            (update-in result [k v] inc)
            ) )
        target_map submap) ) ) )

(defn merge-value-frequencies
  "Merge two value frequency maps where the value frequency totals will be added
  together. Can be used with reducers. For example:
  (merge-value-frequency-maps {:a {a1 1}} {:a {a1 3}}) => {:a {a1 4}}"
  ([] {})
  ([m] m)
  ([mleft mright]
    (merge-with #(merge-with + %1 %2) mleft mright) ) )

(defn- scollect-value-frequencies
  "Determine the cumulative value frequencies for a collection of maps. Single threaded.
  todo: This should be handled by (reduce)."
  [items & {:keys [kset kpath] :or {kset [] kpath []}}]
  (loop [result {}
         m items]
    (if (or (nil? m) (empty? m))
      result
      (recur (merge-value-frequencies result (value-frequencies {} (first m) :kpath kpath :kset kset)) (rest m))
      ) ) )

(defn- pcollect-value-frequencies
  [items & {:keys [kset kpath] :or {kset [] kpath []}}]
  (r/fold
    merge-value-frequencies
    (fn [result item] (merge-value-frequencies result (value-frequencies {} item :kset kset :kpath kpath) ) )
    items
    ) )

(defn collect-value-frequencies
  "Determine the value frequencies for a collection of map structures. Use ':plevel 2' for
  concurrent collection (reducers/fold). For these examples, 'a1', 'b1', 'd1' are strings.

  (collect-value-frequencies [{:a a1} {:a a1 :b b1, :c {:d d1}}])
   => {:a {a1 2} :b {b1 1} :c {{:d1 1} 1}}

  ; Retrieve value frequency for :a
  (collect-value-frequencies [{:a a1} {:a a1 :b b1, :c {:d d1}}] :kset [:a])
   => {:a {a1 2}}

  ; Retrieve value frequency for depth path :c
  (collect-value-frequencies [{:a a1} {:a a1 :b b1, :c {:d d1}}] :kpath [:c] :plevel 2)
   => {:d {d1 1}}"
  [map_items & {:keys [kset kpath plevel] :or {kset [] kpath [] plevel 1}}]
  (if (= 1 plevel)
    ; todo: bring p/scollect into this function. Rework scollect to use (reduce) instead of (loop recur)
    (scollect-value-frequencies map_items :kset kset :kpath kpath)
    (pcollect-value-frequencies map_items :kset kset :kpath kpath)
    ) )

(defn collect-value-frequencies-for
  "Apply a function (fx) to retrieve a list of sub-maps for a complex structure. Use ':plevel 2'
   for concurrent collection (reducers/fold). Where 'a1', 'd1' and 'd2' are strings

  (def m1 {:a a1 :b {:c [{:d d1} {:d d1} {:d d2}]}})
  (collect-value-frequencies-for [m1] #(get-in % [:b :c]))
  => {:d {d2 1 d1 2}}"
  [map_items fx & {:keys [kset kpath plevel] :or {kset [] kpath [] plevel 1}}]
  (if (= 1 plevel)
    (reduce (fn [result item] (merge-value-frequencies result (collect-value-frequencies (fx item))) ) {} map_items)
    (r/fold
      merge-value-frequencies
      (fn [result item] (merge-value-frequencies result (collect-value-frequencies (fx item)) ))
      map_items
      ) ) )

(defn sort-value-frequencies
  "Sort a 'value-frequency' map for each value by frequency (depending).
  (sort-value-frequencies {:a {a1 2, a2 5, a3 1}})
  => {:a {a2 5, a1 2, a3 1}}"
  [vfreqs]
  (reduce
    (fn [result [k v]] (assoc-in result [k] (sort-map-by-value v)) )
    {} vfreqs) )

(defn all?
  "Pass value(s) implicitly and a list of predicates explicitly for evaluation.
  If all predicates return 'true', then function returns 'true'. Otherwise
  function returns 'false'. Could not pass a function list to (every-pred)
  successfully. Ex: ((all? number? odd?) 10) --> false. Resembles (every-pred)"
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
  function returns 'false'. Ex: ((any? number? odd?) 10) --> true. Resembles (some-fn)"
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
  Otherwise returns 'false'. Resembles (some)"
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
  the predicate is satisfied. Otherwise returns an empty list. A lazy implementation
  is slated for 1.7 (this one not be lazy)."
  [pred coll]
  (loop [result '()
         items coll]
    (cond
      (and (empty? items) (= (count coll) (count result))) '()
      (empty? items) result
      (try (pred (first items)) (catch Exception e false)) (conj result (first items))
      :else (recur (conj result (first items)) (rest items))
      ) ) )
