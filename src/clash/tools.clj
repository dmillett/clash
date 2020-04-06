;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
    ^{:author "dmillett"
      :doc "Some potentially useful tools with command.clj or other."}
  clash.tools
  (:require [clojure.core.reducers :as r]
            [clojure.spec.alpha :as cs]
            [clojure.string :as s])
  (:use [clojure.java.io :only (reader)])
  (:import java.text.SimpleDateFormat))

(defn data-to-file
  "Dump a clojure data structure to an EDN file using (with-out-str). This
  requires an existing directory or a filename inclusive of directory."
  [data filename]
  (spit (str filename ".edn") (with-out-str (pr data))) )

(defn data-from-file
  "Read the contents of a small file into its clojure data structure."
  [filename]
  (read-string (slurp filename)))

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
  ([time] (elapsed time "" 4))
  ([time message] (elapsed time message 4))
  ([time message digits]
   (let [p (if (pos? digits) digits 3)]
     (cond
       (< time 99999) (str message "Time(ns):" time)
       (< time 99999999) (str message "Time(ms):" (formatf (millis time) p))
       :else (str message "Time(s):" (formatf (seconds time) p)))
     ) ) )

(defn format-millitime-to
  "Format nano time (9 digits) to a specified date-time format
  uses java SimpleDateFormat."
  [milli_time date_fmt]
  (.. (SimpleDateFormat. date_fmt) (format milli_time)) )

(defmacro latency
  "A macro to determine the latency for function execution. Returns a map
  with ':latency {[:ts :ns]}', ':text', and ':result'"
  [exe & messages]
  `(let [msgs# (str ~@messages)
         start# (System/nanoTime)
         result# ~exe
         time# (nano-time start#)
         scaled# (elapsed time#)]
     {:text (when (not (empty? msgs#)) msgs#) :latentcy {:ts scaled# :ns time#} :result result#} ) )

(def jvm_sysinfo
  "Some information from System/getProperties and Runtime. At a high level:
  processors, heap, os, jvm versions."
  (let [names ["os.name" "os.version" "java.vm.vendor" "java.vm.version" "java.vm.name"
               "java.version" "java.runtime.version" "java.class.version" "java.specification.version"
               "java.vm.specification.version"]
        rt (Runtime/getRuntime)
        ps (.availableProcessors rt)
        mm (.maxMemory rt)
        fm (.freeMemory rt)
        props (select-keys (System/getProperties) names)
        ]
    (merge props {"available.processors" ps, "heap.max.memory" mm, "heap.free.memory" fm})
    ) )

(defmacro repeatfx
  "A macro to evaluate performance of a function over a specified number of executions.
  This is valuable to see how the JVM hotspot optimizes for repeat customers. It is
  possible to collect the result of every execution by setting ':capture true'. Note
  that the JVM hotspot will significantly improve performance over the first 50 iterations.

  user=> (repeatfx 3 (* 3 3) :capture true)
  {:total_time 7729 :values [9 9 9] :averagetime 2576.33}"
  [n fx & {:keys [capture] :or {capture false}}]
  `(loop [i# ~n, ttime# 0, results# []]
     (if (zero? i#)
       {:total_time ttime#
        :average_time (/ ttime# (float ~n))
        :values results#}
       (let [start# (System/nanoTime)
             result# ~fx
             time# (- (System/nanoTime) start#)]
         (recur (dec i#) (+ ttime# time#) (if ~capture (conj results# result#) nil)) )
       ) ) )

(defmacro sweetspot
  "Identify how many repeated executions does it take Hotspot to optimize the call. This macro
  relieas on (repeatfx n fx) to repeatedly execute a function until the 'max_count' or minimum
  performance slope is reached. The following are optional values:

  stepfx - how many times should (repeatfx) run? Defaults #(* 10 %)
  threshold - stop if performance increase is less than this? Defaults to 0.10
  max_count - the maximum step iterations with (repeatfx) to run? Defaults to 10
  verbose - (removed - see 'jvm_sysinfo') dump system information (heap, os, cpus, etc)? Default 'false'
  "
  [fx & {:keys [stepfx delta max_count verbose] :or {stepfx nil delta 0.10 max_count 10 verbose false}}]
  `(let [stepfn# (if ~stepfx ~stepfx #(* 10 %))
         deltafn# #(Math/abs (/ (- %1 %2) %1))
         fxname# (first (map #(str %) '~fx))]
     (loop [i# 0
            total_time# 0
            results# []
            thold# 1.0]
       (if (or (>= i# ~max_count) (<= thold# ~delta))
         {fxname# {:count i# :total total_time# :results results#}}
         (let [n# (stepfn# (inc i#))
               result# (if ~verbose (repeatfx n# ~fx :capture true) (repeatfx n# ~fx))
               previous# (:average_time (last results#))
               current_avg# (:average_time result#)
               current_thold# (if (= 0 i#) 1.0 (deltafn# previous# current_avg#))]
           (recur (inc i#)
                  (+ total_time# (:total_time result#))
                  (conj results# (merge {:n n# :average_time (:average_time result#)}
                                        (if ~verbose (select-keys result# [:values]) nil)))
                  current_thold#)
           ) ) )
     ) )


(defmacro perf
  "Determine function execution time in nano seconds. Display is
  in nanos or millis or seconds (see elapsed()). Println 'time side
  effect. This was first macro I wrote and is functionally equivalent
  to 'latency' function."
  [exe & messages]
  `(let [msgs# (str ~@messages)
         start# (System/nanoTime)
         result# ~exe
         time# (nano-time start#)]
     (if (not (empty? msgs#))
       (println (elapsed time# msgs#))
       (println (elapsed time#)) )
     result#))

(defmacro perfd
  "A debug version of (perf) that will print the optional message, followed
  by (elapsed) time, and then debug: 'result'. Time print out will be scaled via (elapsed).
  todo: It would be nice to link this to a system variable for on/off (println).

  user=> (perfd (+ 1 1))
  debug value: 2 , Time(ns):1567
  2
  "
  [exe & messages]
  `(let [msgs# (str ~@messages)
         start# (System/nanoTime)
         result# ~exe
         total# (nano-time start#)]
     (if (empty? msgs#)
       (println "debug value:" result# "," (elapsed total#))
       (println "debug value:" result# "," msgs# (elapsed total#)) )
     result#))

(defn count-file-lines
  "How many lines in a small file?"
  [file]
  (with-open [rdr (reader file)]
    (count (line-seq rdr))) )

(defn sort-map-by-value
  "Sort a map by value(s) for a given subset and/or path. If sorting by values within
  a nested map, then ':kset' is required.

  (sort-map-by-value {:a 5 :b 3 :c 7})
  {:c 7 :a 5 :b 3}

  (sort-map-by-value {:a 5 :b 3 :c 7} :descending false)
  {:b 3 :a 5 :c 7}

  (sort-map-by-value {:a {:b 3 :c 5} :d {:b 5 :c 1}} :ksubset [:b])
  {:d {:b 5 :c 1} :a {:b 3 :c 5}}

  ;; Add the values from :b :c to drive the sort
  (sort-map-by-value {:a {:b 3 :c 5} :d {:b 5 :c 1}} :ksubset [:b] :datafx #(apply + %))
  {:a {:b 3 :c 5} :d {:b 5 :c 1}}

  ;; Sort by specific keys for nested sub-map
  (sort-map-by-value {:a {:b {:c 34.5 :d 21.3}} :e {:b {:c 21.8 :d 56}}} :ksubpath [:b] :subset [:c :d] :datafx #(apply + %))
  {:e {:b {:c 21.8 :d 56}} :a {:b {:c 34.5 :d 21.3}})

  "
  ; datafx #(if (coll %) (apply str %) (str %)))
  [m & {:keys [descending ksubpath ksubset datafx] :or {descending true ksubpath nil ksubset nil datafx #(identity %)}}]
  (let [ksetfx (fn [k] (if ksubset (into [k] ksubset) [k]))
        valsfx (fn [data k] (let [values (if (map? data) (into [] (vals (select-keys data (ksetfx k)))) data)] (datafx values)))
        kpathfx (fn [k] (valsfx (if ksubpath (get-in m (into [k] ksubpath)) (get m k)) k))
        kfx (fn [k] (apply str (ksetfx k)))]
    (into
      (sorted-map-by
        (fn [k1 k2]
          (if descending
            (compare [(kpathfx k2) (kfx k2)] [(kpathfx k1) (kfx k1)])
            (compare [(kpathfx k1) (kfx k1)] [(kpathfx k2) (kfx k2)])
            ) ) )
    m)))

(defn sort-pivot-by-value
  "Sort a pivot result"
  [pivot_data]
  (into
    (sorted-map-by
      (fn [k1 k2] (compare [(get-in pivot_data [k2 :result]) (str k2)]
                           [(get-in pivot_data [k1 :result]) (str k1)])) )
    pivot_data) )

(defn fns-with
  "Composes higher order functions from a specific function and a collection of
  values. Where args has the following form: {:f afn :v [v1 v2 v3]} or {:f afn :v [[v1 v2] [v3 v4]]}
  Think of using with (all?), (any?), or (none?).  "
  [fx args]
  (let [fn (:f args) v (:v args)]
    (if (coll? (first v))
      (apply fx (map #(apply fn %) v))
      (apply fx (map fn v))
      ) ) )

(defn count-with
  "Tally a count for all of the members of a collection that satisfy the predicate or
  predicate group. Using reducers requires a vecor concurrency. Single threaded is specified
  by :plevel 1. An incrementing function allows for the tallying a specific quantity withing
  a collection data member. The initial count value is zero. This is single threaded by
  default {:plevel 1}. Concurrency starts to show benefits for collections sizes > 40,000.
  The incrementing function has the following form '{:incrfx (fn [current_val result] (somefn))}'
  where 'result' is the first argument from the reduce function.

  If 'sols' or 'preds' is nil, then this returns 0.

  (count-with (range 1 10) (all? number? even?)) => 4
  (count-with (range 1 10) (all? number? even?) :incrfx + :plevel 2) => 20
  ; (+ 5 (+ 0 (* 2 2)))
  ; (+ 9 (* 2 4))
  (count-with {:a 1 :b 2 :c 3 :d 4} even? :initval 5 :incrfx #(+ %2 (* 2 %1))) => 17
  "
  [sols preds & {:keys [incrfx initval plevel] :or {incrfx nil initval 0 plevel 1}}]
  (if (or (nil? sols) (nil? preds))
    0
    (let [reducefx (fn [r i] (if (preds i) (if incrfx (incrfx i r) (inc r)) r))]
      (if (= 1 plevel)
        (if (map? sols) (reduce-kv #(reducefx %1 %3) initval sols) (reduce reducefx initval sols))
        (+ initval (r/fold + (if (map? sols) #(reducefx %1 %3) #(reducefx %1 %2)) sols))
        ) ) ) )

(defn count-from-groups
  "Count all data in a list of groups that satisfies 'predfx'."
  [groups predfx]
  (reduce (fn [result group] (+ result (count-with group predfx))) 0 groups))

(defn- rinto
  "For reducers, will use a list as for 0 or 1 args."
  ([] [])
  ([c1] (vec c1))
  ([c1 c2] (into c1 c2)))



(defn collect-with
  "Build a collection/result set of data that satisfy the conditions defined in
  'predicates'. The predicates should be relevant to use the data structure to filter.
  By default, this will execute single threaded. For concurrent with reducers/fold,
  then in parallel with reducers/fold, specify {:plevel 2} (usually > 40,000 data elements).

  If 'sols' or 'preds' is nil, then this returns an empty list.

  (collect-with (range 0 5) odd?) => '(1 3)
  ; For reducers/fold concurrency, input should be a vector
  (collect-with (into [] (range 0 5)) odd? :plevel 2) => [1 3]
  ; Map data structures will return key-value pairs in a collection
  (collect-with {:a 1 :b 2 :c 3} odd?) => '([:a 1] [:c 3])
  "
  [sols preds & {:keys [plevel] :or {plevel 1}}]
  (if (or (nil? sols) (nil? preds))
    '()
    (if (= 1 plevel)
      (if (map? sols) (filter (fn [[_ v]] (preds v)) sols) (filter preds sols))
      (if (map? sols)
        (r/fold rinto (fn [r k v] (if (preds v) (conj r [k v]) r)) sols)
        (r/fold rinto (fn [r y] (if (preds y) (conj r y) r)) sols)
        ) ) ) )

(defn collect-from-groups
  "Collect all data in a list of groups that satisfies 'predfx'."
  [groups predfx]
  (reduce (fn [result group] (into result (collect-with group predfx))) [] groups))

(defn data-per-thread
  "How many data elements per specified thread. If the number of specified threads
  is greater than available cores, the available cores is used. The number of data
  per core is then calculated by quotient + modulus of size/cores.
  "
  [size n]
  (let [max (.availableProcessors (Runtime/getRuntime))
        t (if (> n max) max n)]
    (+ (quot size t) (rem size t))
    ) )

(defn compare-pivot-with
  "Compare values in two maps with a specific 2 arg function. Currently this assumes
  identical keysets in each map. todo: fix for missing keys (set default value)"
  [m1 m2 compf]
  (reduce
    (fn [result [k v]]
      ; Should compare when the value is not nil
      (let [r (:result v)]
        (assoc result k (when r
                          (compf r (:result (get m2 k))))
                      ) ) )
    {}
    m1) )

(defn compare-map-with
  "Compare values in two maps with a specific 2 arg function. Currently this assumes
  identical keysets in each map. todo: fix for missing keys (set default value)"
  [m1 m2 compf]
  (reduce
    (fn [result [k v]]
      ; Should compare when the value is not nil
      (assoc result k (when v (compf v (m2 k))) ) )
    {}
    m1) )

(defn value-frequencies
  "Find out the frequency of values for each key for a map. This can be useful when
  evaluating a collection of maps/defrecords. Specifying a :kpath will retrieve a map
  at depth, while :kset will limit the result to specific keys. For these examples,
  'a1', 'b1', 'd1' are strings.

  (value-frequencies {:a a1}) => {:a {a1 1}}
  (value-frequencies {} {:a 1 :b {:c1 2}} :kpath [:b]) => {:c {2 1}}"
  ([] {})
  ([m] (value-frequencies {} m))
  ([target_map m & {:keys [kset kpath] :or {kset [] kpath []}}]
    (let [kpmap (if (empty? kpath) m (get-in m kpath))
          mp (if (map? kpmap) kpmap {})
          submap (if (empty? kset) mp (select-keys mp kset))]
      (persistent!
        (reduce
          (fn [result [k v]]
            (assoc! result k {v (inc (get-in result [k v] 0))}) )
          (transient target_map)
          submap)) ) ) )

(defn merge-value-frequencies
  "Merge two value frequency maps where the value frequency totals will be added
  together. Can be used with reducers. For this examples, 'a1', 'b1', 'd1' are strings.

  (merge-value-frequency-maps {:a {a1 1}} {:a {a1 3}}) => {:a {a1 4}}"
  ([] {})
  ([m] m)
  ([mleft mright] (merge-with #(merge-with + %1 %2) mleft mright) ) )

(defn- scollect-value-frequencies
  "Determine the cumulative value frequencies for a collection of maps. Single threaded."
  [items & {:keys [kset kpath] :or {kset [] kpath []}}]
  (reduce
    (fn [result m]
      (merge-value-frequencies result (value-frequencies {} m :kset kset :kpath kpath)))
    {}
    items))

(defn- pcollect-value-frequencies
  "Uses (reducers/fold) to build the result."
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
    (reduce (fn [result item] (merge-value-frequencies result
                                (collect-value-frequencies (fx item) :kset kset :kpath kpath)) ) {} map_items)
    (r/fold
      merge-value-frequencies
      (fn [result item] (merge-value-frequencies result
                          (collect-value-frequencies (fx item) :kset kset :kpath kpath) ))
      map_items
      ) ) )

(defn- mv-freqs-datarow
  "Handle (value-frequencies) for a single {:kp [] :ks []} from (mv-freqs)"
  [data kpsets target]
  (if (empty? kpsets)
    (value-frequencies {} data)
    (reduce
      (fn [result kpset]
        (merge result
          (if-let [fx (:kvfx kpset)]
            (fx data)
            (value-frequencies {} data :kpath (:kp kpset) :kset (:ks kpset))
            )))
      target
      kpsets) ) )

(defn mv-freqs
  "Map value frequencies, is a replacement for (collect-value-frequencies) that supports multiple key-path,
  key-sets per data schema. This supports finding value frequencies for multiple schema keys at different nest levels.

  'kpsets' A vector of maps that detail schema paths and sets or a custom fx
  'kp' A specific schema path
  'ks' The schema keys for the 'kp' above
  'kvfx' A function for retrieving collection data from nested structures, or other, this is painful right now (see below)

  typical usage:
  (mv-freqs data :kpsets [{:kp [:cost] :ks [:amount :tax]} {:kvfx #(some-fn)}])

  Nested array of maps (ugly):
  (mv-freqs data :kpsets [{:kvfx #(mv-freqs (:key %) :kpsets [{:ks [:subkey]}])}])
  "
  [items & {:keys [kpsets target plevel] :or {kpsets [] target {} plevel 1}}]
  (if (= 1 plevel)
    (reduce (fn [result item] (merge-value-frequencies result (mv-freqs-datarow item kpsets target))) {} items)
    (r/fold
      merge-value-frequencies
      (fn [result item] (merge-value-frequencies result (mv-freqs-datarow item kpsets target)))
      items
      ) ) )

(defn sort-value-frequencies
  "Sort a 'value-frequency' map for each value by frequency (depending).
  (sort-value-frequencies {:a {a1 2, a2 5, a3 1}})
  => {:a {a2 5, a1 2, a3 1}}"
  [vfreqs]
  (reduce
    (fn [result [k v]] (assoc result k (sort-map-by-value v)) )
    {} vfreqs) )

(defn filter-value-frequencies
  "Filter the result of (collect-value-frequencies) based on a function for frequency
  keys, values, or both. For example, find all data with an even number of occurrences:

  {:a {\"a1\" 2 \"a2\" 1} :b {\"b1\" 1 \"b3\" 3}}

  (filter-value-frequencies vf (fn [[_ v]] (even? v)))
  => {:a {\"a1\" 2}}
  "
  [data_freqs filterfn]
  (if filterfn
    (reduce (fn [r [k v]]
              (let [fd (into {} (filter filterfn v))]
                (if (empty? fd) r (assoc r k fd))))
      {}
      data_freqs)
    data_freqs
    ) )

(defn reduce-vfreqs
  "Apply a function to a value-frequencies data structure and get a
  value-frequencies structure as a result (todo: explore transducers).

  ; Get the 5 most frequent values for each keypath (use in 'haystack)
  (partial reduce-vfreqs #(take 5 (sort-map-by-value %)))"
  [fx vfreqs]
  (reduce-kv
    (fn [r k v] (assoc r k (into {} (fx v))))
    {}
    vfreqs))

(defn top-freqs
  "The most frequent 'n' value for the applicable keys."
  [n]
  (partial reduce-vfreqs #(take n (sort-map-by-value %))))

(defn bottom-freqs
  "The less frequent 'n' values for the applicable keys."
  [n]
  (partial reduce-vfreqs #(take n (sort-map-by-value % :descending false))))

(defn distinct-by
  "Collect distinct or unique items accoridng to a function 'eqfx'.
  This creates a Map structure where the keys are given by 'eqfx'
  and then returns the values."
  [col eqfx]
  (vals
    (reduce
      (fn [result current]
        (let [k (eqfx current)]
          (if (or (not k) (get result k))
            result
            (assoc result k current)
            ) ) )
      {}
      col)))

;; Notes
; From a performance aspect, the "loop - recur" implementation was about 40% faster when
; used with (pivot-matrix) functions for large data sets. My test case was
; 5,000,000+ rows with 240 combinations of (all? hour-of-day minute-of-hour) or 12,000,000,000 iterations
;
; Sample reducer functions
; (fn [data] (reduce (fn [result p?] (if (p? data) result (reduced false))) true predicates))
;
; (fn [data] (reduce (fn [r p?] (if (and r (p? data)) r (reduced false))) (x? data) predicates))
;
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

(defn none?
  "Returns 'false' if any of the predicates returns true. This could be used with
  (filter) or (count-with), etc. Example
  "
  [& predicates]
  (fn [item]
    (loop [result false
           ps predicates]
      (if (or result (empty? ps))
        (not result)
        (recur ((first ps) item) (rest ps))
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
      (try (pred (first items)) (catch Exception _ false)) true
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
      (try (pred (first items)) (catch Exception _ false)) (conj result (first items))
      :else (recur (conj result (first items)) (rest items))
      ) ) )

(defn consecutive
  "Group data in a collection by a specific function using loop/recur. For example, find
  consecutive even numbers in a collection:

  (consecutive even? [1 2 3 4 6 7 8 4 6 3 12])
  => [[2] [4 6] [8 4 6] [12]]"
  [fx? coll]
  (loop [remain coll, current [], result []]
    (if (empty? remain)
      (if (empty? current) result (conj result current))
      (let [data (first remain)]
        (if (fx? data)
          (recur (rest remain) (conj current data) result)
          (recur (rest remain) [] (if (empty? current) result (conj result current)))
          )))
    ) )
