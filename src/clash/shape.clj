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
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:use [clojure.java.io :only (reader writer)]))

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
    (if (string? xmltext)
      (x/parse (sstream xmltext))
      (x/parse xmltext))
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

  produces:
  {'A.@a' ['a1']
   'A.B.@b' ['b1']
   'A.B' ['boo1', 'boo2']
   'A.C.D ['doo']

   This is an easier way to evaluate what 'keypaths' are present in the data and how frequently
   they occur in a dataset.
  "
  ([xml] (if (string? xml) (flatten-xml (xml-parser xml) "" {}) (flatten-xml xml "" {})))
  ([xmlnode keypath flatd]
   (let [tag (when-let [node_name (:tag xmlnode)] (name node_name))
         content (:content xmlnode)
         kpath (if (empty? keypath) tag (str keypath "." tag))
         atts (apply merge (for [[k v] (:attrs xmlnode)] (assoc {} (str kpath ".@" (name k)) [v])))
         data (merge-data flatd atts)]
     (cond
       (or (nil? xmlnode) (nil? tag) (empty? content)) data
       (map? (first content)) (apply merge-data data (for [node content] (flatten-xml node kpath {})))
       :default (add-keypath-value data kpath (first content))
       ) )
   ) )

(defn flatten-json
  "Flatten a parsed JSON object (cheshire or something similar) and flatten the structure
  into a single depth map. For example:

   (flatten-json (ch/parse-string {\"a\":1, \"b\":{\"c\":3, \"d\":4}}))

   {\"a\" [1], \"b.c\" [3], \"b.d\" [4]}
   "
  ([json] (if (string? json) (flatten-json (json/read-str json) "" {}) (flatten-json json "" {})))
  ([json keypath data]
   {:pre (spec/explain map? json)}
   (let [kfx (fn [kpath k] (cond (empty? kpath) k (empty? k) kpath :default (str kpath "." k)))]
     (reduce
       (fn [result current]
         (let [[k v] (if (= clojure.lang.MapEntry (type current)) current ["" current])]
         (cond
           (vector? v) (merge-data result (flatten-json v (kfx keypath k) {}))
           (map? v) (merge-data result (flatten-json v (kfx keypath k) {}))
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
