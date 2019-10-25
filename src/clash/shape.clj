;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.shape
  (:require [clojure.java.io :as io]
            [clojure.xml :as x]
            ))

; todo: spec?
(defn sstream
  "Convert a String or text to an input stream for parsing"
  [text]
  (when-not (nil? text)
    (io/input-stream (.getBytes text))
    ) )

; todo: spec
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

(defn- merge-data
  "Merge keypaths and data"
  [& ms]
  (apply merge-with
    (fn [r n]
      (if (vector? r)
        (into [] (concat r n))
        [r n]))
    ms) )

; todo: spec
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

; todo: spec
(defn flat-data-frequencies
  "Get the frequency count for each key path in a flattened data structure. That makes it
  possible to sort by frequency and prioritize analysis or code optimization.
  For example:
  {A.@ax [ax1], A.@a [a1], A.B.C.@c [c1 c2]}

  produces:
  {A.@ax 1, A.@a 1, A.B.C.@c 2}
  "
  [flattened_data]
  (reduce (fn [result [k v]] (assoc result k (count v))) {} flattened_data))