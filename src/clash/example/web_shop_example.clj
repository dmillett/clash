;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns clash.example.web_shop_example
  (:require [clash.text_tools :as tt]
            [clojure.string :as s]
            [clojure.java.io :as jio])
  (:use [clash.core]) )

(def simple-structure [:time :action :name :quantity :unit_price])

; 10022013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00
(def detailed-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn weblog-parser
  "An exact parsing of line text into 'simple-structure' using
  'detailed-pattern'."
  [line]
  (tt/regex-group-into-map line simple-structure detailed-pattern) )

(defn name?
  "A predicate to check 'stock' name against the current solution."
  [name]
  (fn [line] (= name (-> line :name))) )

(defn action?
  "A predicate to check the 'search', 'price', or 'purchase' action
  for a shopping service."
  [action]
  (fn [line] (= action (-> line :action))) )

(defn price-higher?
  "If the unit price is higher than X."
  [min]
  (fn [line] (< min (read-string (-> line :unit_price)))) )

(defn price-lower?
  "If the unit price is lower than X."
  [max]
  (fn [line] (> max (read-string (-> line :unit_price)))) )

(defn name-action?
  "A predicate to check :name and :action against the current solution.
  This also works with: (all? (name?) (action?))"
  [name action]
  (fn [line] (and (= name (-> line :name)) (= action (-> line :action)))) )

(defn ts-count-with
  "Uses transduce to count the number of items in a collection that satisfy
  'predfx'. "
  [collection predfx & {:keys [incrfx initval] :or {incrfx nil initval 0}}]
  (if (not (nil? predfx))
    (transduce
      (map (fn [current] (if (predfx current) (if incrfx (incrfx current) 1) 0) ) )
      +
      initval
      collection)
    initval) )

(def increment-with-quanity
  "With (reduce): increments a count based on the :quantity for each solution in the collection"
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )

(def increment-with-quanity2
  "With (transduce): increments a count based on the :quantity for each solution in the collection"
  (fn [solution] (if solution (read-string (-> solution :quantity)) 0) ) )

;; ***** increasing the size of a file to read in
(def load-weblog
  "Load a resource into memory and split by lines"
  (drop 1 (s/split (slurp "test/resources/web-shop.log") #"\n")))

(defn grow-weblog
  "Grow the weblog data by 'n' times and shuffle the output."
  [n data]
  (shuffle (apply concat (repeat n data))))

(defn lines-to-file
  "Write the lines to a file"
  [file data]
  (with-open [wtr (jio/writer file)]
    (doseq [line data] (.write wtr (str line "\n"))) ) )