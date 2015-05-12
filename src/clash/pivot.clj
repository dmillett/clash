;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns ^{:author dmillett} clash.pivot
  (:require [clojure.core.reducers :as r]
            [clojure.math.combinatorics :as cmb]
            [clash.core :as c]
            [clash.tools :as t]) )

(defn- single-pivot-group
  "Create a list of functions given a list of values and add
  meta-data to them with {:base_msg 'msg' :name 'X'}. Any partial function
  that requires multiple params (initially) should satisfie (coll?)"
  [pivotf values msg]
  (if (coll? (first values))
    (map #(with-meta (apply pivotf %) {:name (str msg "-" %)}) values)
    (map #(with-meta (pivotf %) {:name (str msg "-" %)}) values)
  ) )

(defn- combine-functions-with-meta
  "Carry the metadata :name forward from the pivot functions"
  [f preds metafs]
  ; copy meta data from pivot functions when appending them to predicates
  (map #(with-meta
          (apply f (conj preds %))
          {:name (:name (meta %))}) metafs) )

(defn- s-pivot
  ([col preds pivotf pivotd] (s-pivot col t/all? preds pivotf pivotd ""))
  ([col preds pivotf pivotd msg] (s-pivot col t/all? preds pivotf pivotd msg))
  ([col f preds pivotf pivotd msg]
    (let [message (if (empty? msg) "pivot" (str msg "_pivot"))
          fpivots (single-pivot-group pivotf pivotd message)
          combos (combine-functions-with-meta f preds fpivots)]

      (t/sort-map-by-value
        (reduce
          (fn [r fx]
            (assoc-in r [(:name (meta fx))] (t/count-with col fx :plevel 1)) )
          {} combos) )
      ) )  )

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
  ([col preds pivotf pivotd msg] (p-pivot col t/all? preds pivotf pivotd msg))
  ([col f preds pivotf pivotd msg]
    (let [message (if (empty? msg) "pivot" (str msg "_pivot"))
          fpivots (single-pivot-group pivotf pivotd message)
          combos (combine-functions-with-meta f preds fpivots)]

      (t/sort-map-by-value
        (reduce
          (fn [r fx]
            (assoc-in r [(:name (meta fx))] (t/count-with col fx)) )
          {} combos) )
      ) ) )

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
  {is-even-number_pivot-by-2 2, is-even-number_pivot-by-3 1}

  If a pivot predicate (:p) has multiple arity, then the corresponding pivot values (:v) collection
  should also have multiple values, e.g:  :v '([2 3] [4 5])
  "
  [col msg & {:keys [b p v plevel] :or {b [] p nil v [] plevel 1}}]
  (cond
    (= 1 plevel) (s-pivot col t/all? b p v msg)
    (= 2 plevel) (p-pivot col t/all? b p v msg)
    ) )

(defn- s-pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order."
  [col1 col2 preds pivotf pivotd msg compf]
  (let [a (pivot col1 preds pivotf pivotd msg)
        b (pivot col2 preds pivotf pivotd msg)]
    (t/sort-map-by-value (t/compare-map-with a b compf))
    ) )

(defn- p-pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order."
  [col1 col2 preds pivotf pivotd msg compf]
  (let [a (p-pivot col1 preds pivotf pivotd msg)
        b (p-pivot col2 preds pivotf pivotd msg)]
    (t/sort-map-by-value (t/compare-map-with a b compf))
    ) )

(defn pivot-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order. The default parallelism is 1 (single threaded).
  Specify :plevel 2 for parallel operation (r/fold)."
  [col1 col2 msg {:keys [b p v plevel] :or {b [] p [] v [] plevel 1}}]
  (cond
    (= 1 plevel) (s-pivot-compare col1 col2 b p v msg)
    (= 2 plevel) (p-pivot-compare col1 col2 b p v msg)
    ) )

;; **************************************************************************************

(defn- single-pivot-group-matrix
  "Create a list of functions given a list of values and add
  meta-data to them with {:name 'msg'-pivot_X :base_msg 'msg' :pivot 'X'}"
  [pivotf values msg]
  (let [name (str msg "-pivot_")]
    ; :name will get recalculated later when > 1 pivot groups exist
    (if (coll? (first values))
      (map #(with-meta (apply pivotf %) {:name (str name %) :base_msg msg :pivot %}) values)
      (map #(with-meta (pivotf %) {:name (str name %) :base_msg msg :pivot %}) values)
      ) ) )

(defn- build-pg-meta
  "Build a composite meta data string for a predicate group of functions. This
  uses the :base_msg (should be the same for all functions) and then appends
  '-pivots_[x|y]' where 'x' is :pivot for function 1 and 'y' is the :pivot for function 2."
  [pg delim]
  (loop [p pg
         text (str (:base_msg (meta (first p))) "-pivots_[")]
    (if (empty? p)
      (str (subs text 0 (dec (count text))) "]")
      (recur
        (next p)
        (str text (:pivot (meta (first p))) delim))
      ) ) )

(defn- build-matrix
  "A function (all?) followed by a vector of base predicates and a list of list of predicate
  functions (predicate groups)."
  [f base pgs]
  ; make a list of the cartesian products from the predicate groups
  (let [cartesian (apply cmb/cartesian-product pgs)]
    (for [pg cartesian]
      ; apply f, e.g. (all?), to the combintation of base_preds + predicate group
      (with-meta (apply f (into [] (concat base pg))) {:name (build-pg-meta pg "|")})
      ) ) )

(defn- build-pivot-groups-matrix
  "Build a list of pivot predicates for multiple pivots. In this case, each
  param is a sequence. The corresponding index of each sequence are (map)
  together to form a list."
  [pivotfs pivotsd base_msg]
  (loop [result []
         fs pivotfs
         data pivotsd]
    (if (empty? fs)
      result
      (recur
        (conj result (single-pivot-group-matrix (first fs) (first data) base_msg))
        (rest fs)
        (rest data)) )
    ) )

; The name and function that produced a 'count' result
;(defrecord MatrixResult [value function])

(defn- sort-pivot-map-by-value
  "Sort by values descending (works when there are non-unique values too).
  This compares the :value for each MatrixResult"
  [m mkey]
  ; todo: add meta data for later retrieval
  (into (sorted-map-by
          (fn [k1 k2] (compare [(get-in m [k2 mkey]) k2]
                               [(get-in m [k1 mkey]) k1]) ) )
    m) )


; todo: don't store function as metadata, store it next to result
; todo: create printer without 'function'
(defn- compare-pivot-map-with
  "Compare values in two maps with a specific 2 arg function. Currently this assumes
  identical keysets in each map. todo: fix for missing keys (set default value)"
  [m1 m2 f]
  (reduce
    (fn [result [k v]]
      ; Should compare when the value is not nil
      (assoc-in result [k] (when (not (nil? v))
                             {:result (f (:count v) (get-in m2 [k :count]))
                             :function (:function (meta v))})
        ) )
    {} m1) )

(defn- update-map-with-pivot-meta
  "Used to update each new pivot result with meta-data from the previous filter group."
  [m fx col]
  (assoc-in m [(:name (meta fx))] (with-meta {:count (t/count-with col fx :plevel 1)} {:function fx}) ))

(defn- s-pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is single threaded."
  [col msg base_preds pivotfs pivotds]
  (let [message (if (empty? msg) "pivot-test" msg)
        pivot_groups (build-pivot-groups-matrix pivotfs pivotds message)
        flat_matrix (build-matrix t/all? base_preds pivot_groups)]
    (sort-pivot-map-by-value
      (reduce
        (fn [result fx] (update-map-with-pivot-meta result fx col))
         {} flat_matrix)
      :count)
    ) )

(defn- p-pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is in parallel (reducers/fold)."
  [col msg base_preds pivotfs pivotds]
  (let [message (if (empty? msg) "pivot-test" msg)
        pivot_groups (build-pivot-groups-matrix pivotfs pivotds message)
        flat_matrix (build-matrix t/all? base_preds pivot_groups)]
    (sort-pivot-map-by-value
      (reduce
        (fn [result fx] (update-map-with-pivot-meta result fx col))
        {} flat_matrix)
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
  [col msg base_preds pivotfs pivotds]
  (let [message (if (empty? msg) "pivot-test" msg)
        pivot_groups (build-pivot-groups-matrix pivotfs pivotds message)
        flat_matrix (into [] (build-matrix t/all? base_preds pivot_groups))]
    (sort-pivot-map-by-value
      (r/fold reducers-merge
        (fn [results fx] (update-map-with-pivot-meta results fx col))
        flat_matrix)
      :count)
    ) )

(defn pivot-matrix
  "Evaluate a multi-dimensional array of predicates with their base predicates over
  a collection. The predicate evaluation against the collection is single/multithreaded.
  Ex:
  (pivot-matrix-x col [number?] 'foo' :plevel 2 :pivots [divisible-by?] :values [(range 2 5)]

  If a pivot predicate (:p) has multiple arity, then the corresponding pivot values (:v) collection
  should also have multiple values, e.g:  :v '([2 3] [4 5])

  Notes:
  'plevel 1' is single threaded for everything
  'plevel 2' is mutli-threaded when applying predicates to a collection
  'plevel 3' is multi-threaded for list of predicate groups & applying predicates to a collection
             (note: more beneficial for a large cartesian structure or a good multi-cpu workstation)
  )"
  [col msg & {:keys [b p v plevel] :or {b [] p [] v [] plevel 1}}]
  (cond
    (= 1 plevel) (s-pivot-matrix col msg b p v)
    (= 2 plevel) (p-pivot-matrix col msg b p v)
    (= 3 plevel) (pp-pivot-matrix col msg b p v)
    ) )

(defn pivot-matrix-e
  "Identical to, but more explicit than (pivot-matrix). This expects the following form:

  (pivot-matrix-e col msg :base [number? even?] :pivot [{:f divide-by? :v (range 2 5)}
                                                        {:f divide-by? :v (range 5 8)}]
                                                :plevel 2)"
  [col msg & {:keys [base pivot plevel] :or {base [] pivot [] plevel 1}}]
  (let [p (map #(:f %) pivot)
        v (map #(:v %) pivot)]
    (pivot-matrix col msg :b base :p p :v v :plevel plevel)
    ) )

(defn pivot-matrix-compare
  "Compare the results (maps) of two pivots with a specific function. For
  example, perhaps it is helpful to compare the ratio of values from col1/col2.
  The output is sorted in descending order.
  (pivot-matrix-compare col1 col2 msg compf :b common_pred :p pivot_preds :v pivot_values)"
  [col1 col2 msg compf & {:keys [b p v plevel] :or {b [] p [] v [] plevel 2}}]
  (let [pivota (pivot-matrix col1 msg :b b :p p :v v :plevel plevel)
        pivotb (pivot-matrix col2 msg :b b :p p :v v :plevel plevel)]
    (sort-pivot-map-by-value (compare-pivot-map-with pivota pivotb compf) :result)
    ) )

(defn get-rs-from-matrix
  "Get a result set by applying the underlying predicate group (partial function)
  against a data collection. For example, for a pivot matrix with even? divisibly-by? (3 4 5),

  (def hundred (range 1 100))
  (def mtrx (pivot-matrix hundred \"foo\" :b [even?] :p [divisible-by?] :v [(range 2 6]))
  (get-rs-from-matrix hundred mtrx \"foo-pivots_[5]\")
  => (90 80 70 60 50 40 30 20 10)
  "
  [col matrix mkey]
  (if-let [fx (:function (meta (get matrix mkey)))]
    (t/collect-with col fx)
    (t/collect-with col (get-in matrix [mkey :function])) ) )
