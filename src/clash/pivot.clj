;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns ^{:author "dmillett"} clash.pivot
  (:require [clojure.core.reducers :as r]
            [clojure.math.combinatorics :as cmb]
            [clash.tools :as t]
            [clojure.string :as s]) )

(defn- pivot-name
  "Build a pivot name"
  [value message]
  (str message "_[" value "]"))

(defn- single-pivot-group
  "Create a vector of maps with {:fx pivotf:v, :name msg_[v]}. Any partial function
  that requires multiple params (initially) should satisfy (coll?). Example:
  pivotf: divide-by?
  values: [3 4 5]
  --> [{:fx (divide-by? % 3) :name '_[3]'} ...]"
  [pivotf values msg]
  (if (coll? (first values))
    (map (fn [v] {:fx (apply pivotf v) :name (pivot-name v msg)}) values)
    (map (fn [v] {:fx (pivotf v) :name (pivot-name v msg)}) values)
    ) )

(defn- wrap-predicate-group
  "Wrap the pivot functions with another function. Typically (all?) or (any?)."
  [fx preds pivotsfx]
  (map (fn [v] {:fx (apply fx (conj preds (:fx v))) :name (:name v)}) pivotsfx) )

(defn- s-pivot
  ([col preds pivotf pivotd] (s-pivot col t/all? preds pivotf pivotd ""))
  ([col preds pivotf pivotd message] (s-pivot col t/all? preds pivotf pivotd message))
  ([col f preds pivotf pivotd message]
    (let [fpivots (single-pivot-group pivotf pivotd message)
          pivot_fxd (wrap-predicate-group f preds fpivots)]
      (reduce
        (fn [r fxd]
          (assoc r (:name fxd) {:result (t/count-with col (:fx fxd)) :fx (:fx fxd)}) )
        {}
        pivot_fxd)
      ) ) )

(defn- p-pivot
  "Parallel evaluation of each value in a collection (col) with a base set of
  predicates (preds) and a 'pivot' predicate with its list of corresponding
  pivot values. This function returns a map sorted descending by pivot count.
  By default, (pivot) will use the conditional all? (and), but any? (or) could
  also be used. For example:

  ; 6 is an even number dividable by 2, 3
  ; 8 is an even number dividable by 2
  ; 7 is an odd number (it does not satisfy any of the composite predicates)
  user=> (pivot '(6 7 8) [number? even?] divisible-by? '(2 3) \"is-even-number \")

  {is-even-number_pivot-by-2 2, is-even-number_pivot-by-3 1}
  "
  ([col preds pivotf pivotd] (p-pivot col t/all? preds pivotf pivotd ""))
  ([col preds pivotf pivotd message] (p-pivot col t/all? preds pivotf pivotd message))
  ([col f preds pivotf pivotd message]
    (let [fpivots (single-pivot-group pivotf pivotd message)
          pivot_fxd (wrap-predicate-group f preds fpivots)]
      (reduce
        (fn [r fxd]
          (assoc r (:name fxd) {:result (t/count-with col (:fx fxd) :plevel 2) :fx (:fx fxd)}) )
        {}
        pivot_fxd) ) ) )

(defn pivot
  "Evaluation of each value in a collection (col) with a base set of
  predicates (preds) and a 'pivot' predicate with its list of corresponding
  pivot values. This function returns a map sorted descending by pivot count.
  By default, (pivot) will use the conditional all? (and), but any? (or) could
  also be used. For example:

  ; 6 is an even number dividable by 2, 3
  ; 8 is an even number dividable by 2
  ; 7 is an odd number (it does not satisfy any of the composite predicates)
  (pivot '(6 7 8) 'is-even-number' :b [number? even?] :p divisible-by? :v '(2 3))
  or
  (pivot '(6 7 8) 'is-even-number' :b [number? even?] :p divisible-by? :v '(2 3) :plevel 2)
  {is-even-number_[2] 2, is-even-number_[3] 1}

  If a pivot predicate (:p) has multiple arity, then the corresponding pivot values (:v) collection
  should also have multiple values, e.g:  :v '([2 3] [4 5])
  "
  [col msg & {:keys [b p v plevel] :or {b [] p nil v [] plevel 1}}]
  (let [message (if (empty? msg) "pivot" msg)]
    (t/sort-pivot-by-value
      (cond
        (= 1 plevel) (s-pivot col t/all? b p v message)
        (= 2 plevel) (p-pivot col t/all? b p v message)
      ) ) ) )

(defn- s-pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order."
  [col1 col2 basepreds pivotf pivotd msg compf]
  (let [a (pivot col1 msg :b basepreds :p pivotf :v pivotd)
        b (pivot col2 msg :b basepreds :p pivotf :v pivotd)]
    (t/sort-map-by-value (t/compare-pivot-with a b compf))
    ) )

(defn- p-pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order."
  [col1 col2 preds pivotf pivotd msg compf]
  (let [a (pivot col1 msg :b preds :p pivotf :v pivotd :plevel 2)
        b (pivot col2 msg :b preds :p pivotf :v pivotd :plevel 2)]
    (t/sort-map-by-value (t/compare-pivot-with a b compf))
    ) )

(defn pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order. The default parallelism is 1 (single threaded).
  Specify :plevel 2 for parallel operation (r/fold).

  (pivot-compare c1 c2 \"divy1\" > :b [number?] :p divisible-by? :v [2 3 4])
  ; {divy1_[2] 10, divy1_[3] 7, divy1_[4] 5}
  ; {divy1_[2] 10, divy1_[3] 6, divy1_[4] 5}
  => {\"divy1_[3]\" true, \"divy1_[4]\" false, \"divy1_[2]\" false}
  "
  [col1 col2 msg compfx & {:keys [b p v plevel] :or {b [] p nil v [] plevel 1}}]
  (cond
    (= 1 plevel) (s-pivot-compare col1 col2 b p v msg compfx)
    (= 2 plevel) (p-pivot-compare col1 col2 b p v msg compfx)
    ) )

;; **************************************************************************************

(defn pivot-matrix-str
  "Generate a list of strings from a pivot-matrix result in the following format:
  'key': k, 'count:' count"
  [pivot_matrix]
  (map (fn [[k v]] (str "key: " k ", count: " (:count v))) pivot_matrix))

(defn print-pivot-matrix
  "Print a less verbose pivot matrix result without generated :function."
  ([pivot_matrix] (print-pivot-matrix pivot_matrix "\n"))
  ([pivot_matrix delim]
   (print (interpose delim (pivot-matrix-str pivot_matrix))) ) )

(defn sort-pivot-map-by-value
  "Sort by values descending (works when there are non-unique values too).
  This compares the :value for each MatrixResult"
  [m mkey]
  (into (sorted-map-by
          (fn [k1 k2] (compare [(get-in m [k2 mkey]) k2]
                               [(get-in m [k1 mkey]) k1]) ) )
        m) )


(defn compare-pivot-map-with
  "Compare values in two maps with a specific 2 arg function. Currently this assumes
  identical keysets in each map. todo: fix for missing keys (set default value)"
  [m1 m2 f]
  (reduce
    (fn [result [k v]]
      (assoc result k (when-not (nil? v)
                        {:result (f (:count v) (get-in m2 [k :count]))
                         :function (:function v)}) ) )
    {}
    m1) )

(defn- create-single-pivot-group ; todo: make common with pivot
  "Create a list of functions given a list of values and add
  meta-data to them with {:fx pivotfx :name 'msg'-pivot_X :base_msg 'msg' :pivot 'X'}"
  [pivotf values msg]
  (if (coll? (first values))
    (map (fn [v] {:fx (apply pivotf v) :base_msg msg :pivot v}) values)
    (map (fn [v] {:fx (pivotf v) :base_msg msg :pivot v}) values)
    ) )

(defn- build-pivot-groups
  "Build a list of pivot predicates for multiple pivots. In this case, each
  param is a sequence. The corresponding index of each sequence are (map)
  together to form a list."
  [pivotfs pivotsd base_msg]
  (loop [result [], fs pivotfs, data pivotsd]
    (if (empty? fs)
      result
      (recur
        (conj result (create-single-pivot-group (first fs) (first data) base_msg)), (rest fs), (rest data))
      ) ) )

(defn- pivot-matrix-name
  "Build a composite meta data string for a predicate group of functions. This
  uses the :base_msg (should be the same for all functions) and then appends
  '-pivots_[x|y]' where 'x' is :pivot for function 1 and 'y' is the :pivot for function 2."
  [pg delim]
  (loop [p pg
         text (str (:base_msg (first p)) "_[")]
    (if (empty? p)
      (str (subs text 0 (dec (count text))) "]")
      (recur
        (next p)
        (str text (:pivot (first p)) delim))
      ) ) )

(defn- build-pivot-matrix
  "A function (all?) followed by a vector of base predicates and a list of list of predicate
  functions (predicate groups). Generates a cartesian product based list of every predicate
  group combination and uses each item with (all?)"
  [f base pgs]
  (let [cartesian (vec (apply cmb/cartesian-product pgs))]
    (for [pg cartesian]
      {:fx (apply f (into base (map :fx pg))) :name (pivot-matrix-name pg "|")}
      ) ) )

(defn- execute-pivot-group
  "Used to update each new pivot result with meta-data from the previous filter group."
  [m fx col plevel]
  (assoc m (:name fx) {:count (t/count-with col (:fx fx) :plevel plevel) :function (:fx fx)}) )

(defn- s-pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is single threaded."
  [col message combfx? basefx? pivotfx? pivotds]
  (let [pivot_groups (build-pivot-groups pivotfx? pivotds message)
        flat_matrix (build-pivot-matrix combfx? basefx? pivot_groups)]
    (sort-pivot-map-by-value
      (reduce
        (fn [result fxdata] (execute-pivot-group result fxdata col 1))
        {}
        flat_matrix)
      :count)
    ) )

(defn- p-pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is in parallel (reducers/fold)."
  [col message combfx? basefx? pivotfx? pivotds]
  (let [pivot_groups (build-pivot-groups pivotfx? pivotds message)
        flat_matrix (build-pivot-matrix combfx? basefx? pivot_groups)]
    (sort-pivot-map-by-value
      (reduce
        (fn [result fx] (execute-pivot-group result fx col 2))
        {}
        flat_matrix)
      :count)
    ) )

(defn- reducers-merge
  ([] {})
  ([& maps] (merge maps)) )

(defn- pp-pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is in parallel (reducers/fold).
  This might be beneficial when the flattened cartesian product has a large count
  (maybe > 50 predicate groups) and the workstation has a large number of cores."
  [col message combfx? basefx? pivotfx? pivotds]
  (let [pivot_groups (build-pivot-groups pivotfx? pivotds message)
        flat_matrix (vec (build-pivot-matrix combfx? basefx? pivot_groups))]
    (sort-pivot-map-by-value
      (r/fold reducers-merge
        (fn [results fx] (execute-pivot-group results fx col 2))
        flat_matrix)
      :count)
    ) )

(defn pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is single/multithreaded.
  Ex:
  (pivot-matrix col [number?] 'foo' :plevel 2 :p [divisible-by?] :v [(range 2 5)]

  If a pivot predicate (:p) has multiple arity, then the corresponding pivot values (:v) collection
  should also have multiple values, e.g:  :v '([2 3] [4 5])

  Notes:
  'plevel 1' is single threaded for everything
  'plevel 2' is mutli-threaded when applying predicates to a collection
  'plevel 3' is multi-threaded for list of predicate groups & applying predicates to a collection
             (note: more beneficial for a large cartesian structure or a good multi-cpu workstation)
  )"
  [col msg & {:keys [c b p v plevel] :or {c t/all? b [] p [] v [] plevel 1}}]
  (let [message (if (empty? msg) "pivot" msg)]
    (cond
      (= 1 plevel) (s-pivot-matrix col message c b p v)
      (= 2 plevel) (p-pivot-matrix col message c b p v)
      (= 3 plevel) (pp-pivot-matrix col message c b p v)
      ) ) )

(defn pivot-matrix*
  "The preferred version of (pivot-matrix) manifestations. Identical function, but more explicit than (pivot-matrix).
  This introduces 'combfx?' which is a function to apply across each generated predicate group. Previous incarnations
  used (clash.tools/all?), while this allows for (any?), (none?), etc. The default behavior is still (all?).

  keywords:
  :basefx? - applies these predicates to entire data collection. Example: (all? number? even?), Defaults to []
             (decreases the initial collection size - also unnecessary DEPRECATED)
  :combfx? - combfx which function to use to to combine generated fuctions. Defaults to (all?)
  :refine - reduce the collection size when paired with (all?). Set ':refine false' when using (none?)
  :plevel - level of parallel processing. Default, single threaded (1); battery crusher, all threads (2)
  :pivots - A vector of function-data pairs. Use {:f divide-by? :v (range 1 10)}. Defaults to []

  (pivot-matrix* col, msg, :plevel 2, :basefx [number? even?] :pivots [{:f divide-by? :v (range 2 5)}
                                                          {:f divide-by? :v (range 5 8)}])"
  [col msg & {:keys [basefx? pivots combfx? refine plevel] :or {basefx? [] pivots [] combfx? t/all? refine true plevel 1}}]
  (let [p (map :f pivots)
        v (map :v pivots)
        ; todo: explore transducer here
        datacol (if (empty? basefx?) col (t/collect-with col (apply t/all? basefx?) :plevel plevel))
        pvfns (map #(t/fns-with t/any? %) pivots)
        refined (if refine (t/collect-with datacol (apply t/all? pvfns) :plevel plevel) datacol)
        ]
    (pivot-matrix refined msg :c combfx? :b [] :p p :v v :plevel plevel)
    ) )

(defn pivot-matrix-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order.
  (pivot-matrix-compare col1 col2 msg compf :b common_pred :p pivot_preds :v pivot_values)"
  [col1 col2 msg compfx & {:keys [c b p v plevel] :or {c t/all? b [] p [] v [] plevel 2}}]
  (let [pivota (pivot-matrix col1 msg :c c :b b :p p :v v :plevel plevel)
        pivotb (pivot-matrix col2 msg :c c :b b :p p :v v :plevel plevel)]
    (sort-pivot-map-by-value (compare-pivot-map-with pivota pivotb compfx) :result)
    ) )

(defn pivot-rs
  "Duplicates (get-rs-from-matrix), but with a better function name.

  (def hundred (range 1 100))
  (def mtrx (pivot-matrix* hundred \"foo\" :basefx [even?] :pivots [{:f divisible-by? :v (range 2 6)})
  (get-rs-from-matrix hundred mtrx \"foo-pivots_[5]\")
  => (90 80 70 60 50 40 30 20 10)
  "
  [col pivot_matrix pivot_key]
  (if-let [fx (:function (meta (get pivot_matrix pivot_key)))]
    (t/collect-with col fx)
    (t/collect-with col (get-in pivot_matrix [pivot_key :function]))
    ) )

(defn filter-pivots
  "Find pivot results that match specific terms. To find a range of
  specific result keys, pass in a collection of string terms to match.
  Filter result counts based on a defined function 'cfx'.

  ; Return all the results with 'even' counts
  (filter-pivots pm :cfx even?)

  ; Return all the results with 'a' and '9' in the key
  (filter-pivots pm :kterms [\"a\" \"9\"])
  "
  [pivot_matrix & {:keys [kterms cfx] :or {kterms [] cfx nil}}]
  (if (and (empty? kterms) (nil? cfx))
    pivot_matrix
    (let [fx1 (fn [[k _]] (every? #(.contains k %) kterms))
          fx2 (fn [[_ v]] (cfx (:count v)))
          fx3 #(apply merge (map (fn [[k v]] {k v}) %))]
      (cond
        (nil? cfx) (fx3 (filter fx1 pivot_matrix))
        (empty? kterms) (fx3 (filter fx2 pivot_matrix))
        :else (fx3 (filter (t/all? fx1 fx2) pivot_matrix))
        ) )
    ) )

(defn- haystack-keypaths
  "Determine the appropriate schema key paths from value frequencies and the original criteria."
  [kpsets freq_keys]
  (mapcat
    (fn [kps]
      (let [kp (:kp kps) ks (:ks kps)]
        (cond
          (and kp ks) (map #(conj kp %) ks)
          kp (map #(conj kp %) freq_keys)
          ks (map vector ks)
          ) ) )
    kpsets))

(defn haystack
  "Correlate the value of specific data schema 'keys', based on frequency, across a collection of data.
  This combines (collect-value-frequencies) and (pivot-matrix) functions, and only requires knowledge
  of the data schema and constraints about the data values.

  Ex (find out what time the largest product markups sold):
  (haystack purchases :vfkpsets [{:kp [:time] :ks [:hour :minute]} {:kp [:total] :ks [:markup]}])

  map_data: a collection of map/defrecord/etc data

  pvmsg: a base message used for. See '(pivot-matrix)
  pvpredfx: a specific pivot function:value group
  pivots: [{:f some-fn :v fn-args}] explicit pivots similar to what's constructed in (pivot) or (pivot-matrix)
  pvfx: a comparison function generated with value frequency data and row data, defaults to (=)

  vfkpsets: the nested structure key paths/sets within each data. See '(mv-freqs)
  vffx: a 1 arg function that can be applied to the entire key/value groups (ex: filter, sort, etc)

  plevel: concurrency. Default to 1 (single threaded). Use 2 for all threads.
          See '(pivot-matrix), '(collect-value-frequencies)"
  [map_data & {:keys [pvmsg pvpredfx pivots pvfx vfkpsets vffx plevel]
               :or {pvmsg "haystack", pvpredfx t/all?, pvfx =, plevel 1}}]
  (let [vfreqs (t/mv-freqs map_data :kpsets vfkpsets :plevel plevel)
        ffreqs (if vffx (vffx vfreqs) vfreqs)
        keypaths (haystack-keypaths vfkpsets (keys ffreqs))
        pivot_kfx (fn [k v] #(pvfx v (get-in % k)))
        pivot_fxs (map (fn [k] (partial pivot_kfx k)) keypaths)
        pivot_vals (map keys (vals ffreqs))
        pivotfns (concat (map :f pivots) pivot_fxs)
        pivotvals (if pivots (conj pivot_vals (map :v pivots)) pivot_vals)
        msg2 (str pvmsg "(" (s/join "|" keypaths) ")")
        pivots (reduce-kv (fn [r k v] (conj r {:f k :v v})) [] (zipmap pivotfns pivotvals))
        ]
    (pivot-matrix* map_data msg2 :pivots pivots :c pvpredfx :plevel plevel)
    ) )
