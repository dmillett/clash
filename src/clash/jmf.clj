;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.jmf
  (:require [clash.tools :as ct]
            [clojure.set :as set]
            [clojure.string :as cls]
            [clash.text_tools :as ctt])
  (:import (java.util Map)))

(defn- setify
  "Make a set out of a value unless it is already a Set."
  [x]
  (if (set? x)
    x
    (set x)))

;; Join Merge Filters (jmf) are used with transducers to maintain some
;; level of state across the data stream. It might be collecting specific
;; counts of data types, filtering specific data, etc. It should be used
;; after basic transformations of the underlying data.

(defn jmf-word-count
  "Count words across all/some lines. Secondary word pattern fields
  might also be used. This is useful for a larger text blob.
  "
  [& {:keys [regex lcase?] :or {regex #"[\w'\-]+" lcase? true}}]
  (fn
    ([])
    ([results] results)
    ([results values]
     (let [words (ctt/word-count values :regex regex :lcase? lcase?)]
       (merge-with + results words)
       ) ) ) )

(defn jmf-kv-regex-count
  "Look at specific keys in a key:value pair structure (Map),
  and perform a regular expression count on the values. For example,
  use this to look at guest comments and see which words, etc appear
  most frequently.
  "
  [keypaths regex]
  (fn
    ([] {})
    ([results] results)
    ([results data]
      (reduce-kv
        (fn [r k v]
          (assoc r k (regex (apply str v)))
          )
        results
        (select-keys data keypaths))
      ) ) )

(defn jmf-value-regex
  "Combine with a transducer across streaming data to determine:
  1. What the regular expression represents this data value
  2. Add or update the running count of regular expressions for these values

  a.b -> 1 --> \\d
  a.b -> 1, 3 --> \\d\\,\\s\\d
  a.b -> Hello 2 --> \\w{5}\\s\\d
  a.b -> 3 --> \\d

  {\\d 2, \\d\\,\\s\\d 1, \\w{5}\\s\\d}

  This implementation uses flattened data paths (key:value) from JSON, XML, etc

  "
  [fx-regex]
  (fn
    ([] {})
    ([results] results)
    ([results flattened]
     (ct/merge-value-frequencies results
       (reduce-kv
         (fn [r k v]
           (let [rexs (map #(fx-regex (cls/trim (ctt/to-text %))) v)
                 grouped (reduce
                           (fn [r1 v1] (if (get r1 v1) (update-in r1 [v1] inc) (assoc r1 v1 1)))
                           {}
                           rexs)]
             (assoc r k grouped)
             ) )
         {}
         flattened)
       ) ) ) )

;;(defn jmf-word-patterns
;;  "Examine the value in key-value pairs for specific word patterns
;;  that contain specific punctuation. The data and punctuation should
;;  both be key value structures."
;;  [data punctuation & {:keys [kfx? vfx?] :or {kfx nil vfx nil}}]
;;  (fn
;;    ([])
;;    ([results] results)
;;    ([results values]
;;     (reduce-kv
;;       (fn [r k v]
;;         (if (not (empty? punctuation))
;;           (reduce-kv (fn [r1 k1 v1] (assoc r1 k1 (frequencies (re-seq v1 v))) )
;;             r
;;             punctuation)
;;           r) )
;;       results
;;       values) ) ) )

(defn jmf-merge-kv-filter
  "Filter which key:value data is kept/accumulated as it transverses the stream.
  This can be applied to flattened xml/json/etc data to examine specific
  keys or values from the larger schema set. If the key or value filter returns
  true, then that pair will be included
  "
  [key-filter value-filter]
  (fn
    ([] {})
    ([results] results)
    ([results data]
      ;; How to evaluate a method is null just once
      (reduce-kv
        (fn [r k v] (if (or (and key-filter (key-filter k))
                            (and value-filter (value-filter v)))
                      (merge-with concat r {k v})
                      r) )
        results
        data)
      ) ) )
