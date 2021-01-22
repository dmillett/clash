;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.shape
  (:require [clojure.spec.alpha :as spec]
            [clojure.java.io :as io]
            [clojure.xml :as x]
            [cheshire.core :as cc]
            [clash.tools :as ct]
            [clojure.string :as s]))

(defn sstream
  "Convert a String or text to an input stream for parsing"
  [text]
  (when-not (nil? text)
    (io/input-stream (.getBytes text))
    ) )

(defn xml-parser
  "Use clojure.xml to parse a string or an input-stream."
  [xmltext]
  (try
    (when (and (not-empty xmltext) (s/starts-with? xmltext "<"))
    (if (string? xmltext)
      (x/parse (sstream xmltext))
      (x/parse xmltext)))
     (catch Exception e (println e)
     ) ) )

(defn- add-keypath-value
  "Add data value for a specific keypath. Append value if
  the key already exists."
  [data keypath value]
  (if-let [entry (get data keypath)]
    (assoc data keypath (conj entry value))
    (assoc data keypath [value])
    ) )

(defn merge-data
  "Merge keypaths and data. If the keypath already exists,
  then the data is appended to a vector."
  [& ms]
  (apply merge-with
    (fn [values v]
      (if (vector? values)
        (into [] (concat values v))
        [values v]))
    ms) )

(defn flatten-xml
  "This takes xml text or xml data (via clojure.xml) to flatten into key:value map that
  determines what the nested XML structure looks like and what values it has. For example:

  <A a='a1'>
  <B>boo1</B>
  <B b='b1'>boo2</B>
  <C>
    <D>doo</D>
  </C>
  </A>

  (flatten-xml <xml-from-above>)
  {\"A.@a\" [\"a1\"] \"A.B.@b\" [\"b1\"] \"A.B\" [\"boo1\", \"boo2\"] \"A.C.D\" [\"doo\"]}

   ;; Describing the underlying structure using '{}'
   (flatten-xml \"<A><B><C>c1</C><C>c2</C></B></A>\" true)
   => {\"A{}.B{}.C\" [\"c1\" \"c2\"]}

   This is an easier way to evaluate what 'keypaths' are present in the data and how frequently
   they occur in a dataset.
  "
  ([xml] (if (string? xml) (flatten-xml (xml-parser xml) "" {}) (flatten-xml xml "" {})))
  ([xml describe?] (if (string? xml) (flatten-xml (xml-parser xml) "" {} describe?) (flatten-xml xml "" {} describe?)))
  ([xmlnode keypath flatd] (flatten-xml xmlnode keypath flatd false))
  ([xmlnode keypath flatd describe?]
   (let [tag (when-let [node_name (:tag xmlnode)] (name node_name))
         content (:content xmlnode)
         kpath (if (empty? keypath) tag (str keypath "." tag))
         atts (apply merge (for [[k v] (:attrs xmlnode)] (assoc {} (str kpath ".@" (name k)) [v])))
         data (merge-data flatd atts)]
     (cond
       (or (nil? xmlnode) (nil? tag) (empty? content)) data
       (map? (first content)) (apply merge-data data (for [node content] (flatten-xml node (if describe? (str kpath "{}") kpath) {} describe?)))
       :default (add-keypath-value data kpath (first content))
       ) )
   ) )

(defn to-json
  "Parse string json form into JSON or just return JSON structure"
  [json_data]
  (if (string? json_data)
    (cc/parse-string json_data)
    json_data
    ) )

(defn flatten-json
  "Flatten a parsed JSON object (cheshire or something similar) and flatten the structure
  into a single depth map. For example:

   (flatten-json (ch/parse-string {\"a\":1, \"b\":{\"c\":3, \"d\":4}}))
   => {\"a\" [1], \"b.c\" [3], \"b.d\" [4]}

   ;; Describes 'a.b' as a map structure
   (flatten-json (ch/parse-string {\"a\":1, \"b\":{\"c\":3, \"d\":4}}) true)
   => {\"a\" [1], \"b{}.c\" [3], \"b{}.d\" [4]}
   "
  ;; backward compatibility fun -- to include 'describe'?
  ;([json & {:keys [keypath data describe] :or {keypath "" data {} describe false}}])
  ([json] (flatten-json (to-json json) "" {}))
  ([json describe?] (flatten-json (to-json json) "" {} describe?))
  ([json keypath data] (flatten-json json keypath data false))
  ([json keypath data describe?]
   {:pre (spec/explain map? json)}
   (let [kfx (fn [kpath k]
               (cond
                 (empty? kpath) k
                 (empty? k) kpath
                 :default (str kpath "." k)))
         kfx2 (fn [k sym]
                (if (and describe? (not (s/blank? k)))
                  (str k sym)
                  k))
         ]
     (reduce
       (fn [result current]
         (let [[k v] (if (map-entry? current) current ["" current])]
           (cond
             (vector? v) (merge-data result (flatten-json v (kfx keypath (kfx2 k "[]") ) {} describe?))
             (map? v) (merge-data result (flatten-json v (kfx keypath (kfx2 k "{}")) {} describe?))
             :default (add-keypath-value result (kfx keypath k) v)
             )))
       data
       json)
      )))

(defn flat-data-value-counts
  "Get the value counts for each key path in a flattened data structure. That makes it
  possible to sort by frequency and prioritize analysis or code optimization. Key paths
  that represent arrays will have larger 'counts'. A key path might have low frequency,
  but a lot of data.

  For example:
  {A.@ax [ax1], A.@a [a1], A.B.C.@c [c1 c2]}

  produces:
  {A.@ax 1, A.@a 1, A.B.C.@c 2}
  "
  [flattened_data]
  {:pre (spec/explain map? flattened_data)}
  (reduce
    (fn [result [k v]] (assoc result k (count v)))
    {}
    flattened_data))

(defn keypath-frequencies
  "The frequency of keypaths for a large collection of flattened data."
  ([] {})
  ([freqs] freqs)
  ([freqs flat]
    (reduce
      (fn [result [k _]]
        (if-let [i (get result k)]
          (assoc result k (inc i))
          (assoc result k 1)
          ) )
      freqs
      flat)))

(defn- as-string
  "Return the string value of an object. 1 --> \"1\""
  [o]
  (if (and o (string? o)) o (str o)))

;; Use as an ordered list in (value-pattern)
(defrecord ValuePattern [type pattern fx])

;; 2020-03-28T17:07:31.000
(def simple_patterns
  "A simple example of regular expressions to use with flattened data to provide shape of data values."
  [(->ValuePattern :financial #"[A-Z]{2,3}\s*[\d\.]+" nil)
   (->ValuePattern :datetime #"\d{2,4}\-\d{2}\-\{2,4}[T\d:\.]+" nil)
   (->ValuePattern :decimal #"\d*\.\d+" nil)
   (->ValuePattern :boolean #"true|false" (fn [v] (s/lower-case v)))
   (->ValuePattern :int #"\d+" nil)
   (->ValuePattern :email #"\s*\w+@\w+\.\w+\s*" nil)
   (->ValuePattern :text #"[\w\s.]+" nil)
   (->ValuePattern :any #".+" nil)
   (->ValuePattern :empty #"" nil)
   ])

(defn value-pattern
  "Check the stringified value against a dictionary of regular expressions to find the match. For starters,
  use (->ValuePattern x y z) or see (simple_patterns)

  Each pattern for evaluation should have: ':name', ':pattern', and ':fx' (optional)
  (value-pattern \"hello dave\" [{:name :int, :pattern #\"\\d+\"} {:name :text, :pattern #\"[\\w\\s]+\"}])

   If 'value' is nil, then :no_match is returned

   This is helpful for determining rough/fine data categorization over a larger group of data.
   **NOTE** Specify regex from most to least specific for 'patterns'
   "
  [value patterns]
  (if-let [v (as-string value)]
    (reduce
      (fn [_ tp]
        (if (re-find (:pattern tp) (if-let [f (:fx tp)] (f v) v))
          (reduced (:type tp))
          :no_match))
      nil
      patterns)
    :no_match))

(defn- coll-patterns
  "Find how many pattern matches there are for a collection of data."
  [patterns coll]
  (frequencies (for [v coll] (value-pattern v patterns))) )

(defn- update-pattern-freqs
  "Determine the pattern counts for all of the values for a specific key."
  [patterns result data]
  (reduce-kv
    (fn [r k v]
      (let [shapes (get result k) values (coll-patterns patterns v)]
        (if shapes
          (assoc r k (merge-with + shapes values))
          (assoc r k values)
          ) ) )
    result
    data))

(defn keypath-value-patterns
  "Use 'shape_patterns' to identify what values are associated with each keypath. For example:

  [\"a\" 1 2.0 true \"b\" \"USD 2.50\"] --> {:small_text 2, :int 1, :decimal 1, :boolean 1, :financial 1}
  "
  [patterns & args]
  (cond
    (= 1 (count args)) (first args)
    (= 2 (count args)) (update-pattern-freqs patterns (first args) (last args))
    :default {}
    ) )

(defn- map-value-total
  [m]
  (reduce-kv
    (fn [r _ v] (+ r v))
    0
    m))

(defn shape-sort-value-pattern
  "Sort by value pattern counts for 'shaped' data in descending order.

  {:foo {:int 3 :text 6 :decimal 4}} --> {:foo {:text 6 :decimal 4 :int 3}}
  "
  [shaped]
  (reduce-kv
    (fn [r k v] (assoc r k (ct/sort-map-by-value v)))
    {}
    shaped))

(defn shape-sort-keypath-count
  "Sort by keypath occurrence frequency in descending order.

  (shape-sort-keypath-count {:foo {:int 1 :decimal 1} :bar {:decimal 3 :text 4}})
  => {:bar {:decimal 3 :text 4} :foo {:int 1 :decimal 1}}
  "
  [shaped]
  (into
    (sorted-map-by
      (fn [k1 k2]
        (compare [(map-value-total (get shaped k2)) (str k2)]
                 [(map-value-total (get shaped k1)) (str k1)])))
    shaped))

(defn shape-sort
  "Sort by keypath frequency and value pattern frequency in descending order.

  (shape-sort {:foo {:int 1 :decimal 2} :bar {:decimal 3 :text 4}})
  => {:bar {:text 4 :decimal 3} :foo {:decimal 2 :int 1}}
  "
  [shaped]
  (shape-sort-keypath-count (shape-sort-value-pattern shaped)))

(def simple-json-parser
  "A simple JSON parser that skips empty strings and dumps 'Error:' for any poorly formatted JSON.
  The JSON text is curried from context."
  (fn [line] (try (when (not-empty line) (flatten-json line)) (catch Exception _ (println "Error:" line)))))

(defn xml-and-json-parser
  "When the input data (line) is JSON OR XML, this parser will handle both."
  [line]
  (try
    (when (not-empty line)
      (cond
        (s/starts-with? line "<") (flatten-xml line)
        (s/starts-with? line "{") (flatten-json line)
        :default (println "Skipping line:" line)))
    (catch Exception e (println "Error:" e))))

;; todo macro?
(defn- todo
  ([text] (todo text nil))
  ([text fx] (todo text fx nil))
  ([text fx & args]
    (println text " args:" args)))

(defn- strip-symbol
  [text symbol]
  (s/replace text symbol ""))

(defn- specmap
  [nspace specname]
  (fn [required optional]
    ;(s/def specname :req required :opt optional)
    ) )

(def specdefs
  {:map (fn [] (spec/def ::map #(map? %)))
   :array (spec/def ::array #(vector? %))
   :attribute (spec/def ::attribute #())
   })

;; Return a function if for the first two defintions, otherwise the result
; todo: return functions, similar to transducer intermediates
(defn foobar
  ([mp] (fn [req] (foobar mp req nil)))
  ([mp req] (foobar mp req nil))
  ([mp req opt]

   ) )

;; A map of keys --> spec definitions
; key --> s/def
; todo: should be a transducer-tree structure
(defn spec-from
  "Build a Spec from flattened and described data.

  {a{}.b{}.c[] [1 2 3]
   a{}.b{}.d [true]
   a{}.b{}.e{}.f[] [4 5 6]}

   {a{} {
  "
  [flat_data_shape]
  (reduce-kv
    (fn [result k v]
      (let [paths (s/split k #"\.")]
        (reduce
          (fn [r1 path]
            (cond ;; Figure out what spec type each is
              (s/includes? path "{}") (todo "map spec" (assoc r1 path (:map specdefs)) path)
              (s/includes? path "[]") (todo "vector spec" (assoc r1 path (:array specdefs)) path)
              (s/includes? path "@") (todo "attribute spec (xml only)" (assoc r1 path (:attribute specdefs)) path)
              :default (todo "N/A (is a key within the encapsulating data structure)" nil path)
              ))
          result
          paths)
        (todo "Merge/associate with 'result'" #(assoc result k %) k result)
        ) )
    {}
    flat_data_shape))

;;
; (def data1 (flatten-json "{\"a\":1, \"b\":{\"b1\":[2,3], \"b2\":true}}" true))
; {a [1], b{}.b1[] [2 3], b{}.b2 [true]}
; Spec-tree:
; (s/def ::data1 (s/keys :req ["a" "b"] :opt []))
;   - (s/def ::a int?)
;   - (s/def ::b (s/keys :req ["b1"] :opt []))
;     - (s/def ::b1

;;
; (s/valid? even? 1) --> false
; (s/valid? even? 2) --> 2
; (s/valid? fx? value) --> value or false
; (s/conform even? 1) --> clojure.spec.alpha/invalid
; (s/conform even? 2) --> 2
; (s/valid? (s/nilable number?) nil) --> true
; (s/explain (s/nilable number?) "a") --> "a" - failed: number? at
;
; (s/def ::first-name string?)
; (s/def ::last-name string?)
; (s/def ::person (s/keys :req [::first-name ::last-name] :opt [::age]))
;