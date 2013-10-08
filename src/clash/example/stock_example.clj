;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.example.stock_example
  (:require [clash.text_tools :as tt])
  (:use [clash.interact]) )

(def simple-stock-structure [:trade_time :action :stock :quantity :price])

; 05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00
(def detailed-stock-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn is-buy-or-sell?
  "If the current line contains 'Buy' or 'Sell'. "
  [line]
  (if (or (tt/str-contains? line "Buy") (tt/str-contains? line "Sell"))
    true
    false) )

(defn simple-message-parser
  "An inexact split and parse of line text into 'simple-stock-structure'."
  [line]
  (let [splitsky (tt/split-with-regex line #"\|")
        date (first splitsky)
        message (last splitsky)
        corrected (str date "," message)]
    (tt/text-structure-to-map corrected #"," simple-stock-structure)) )

(defn better-message-parser
  "An exact parsing of line text into 'simple-stock-structure' using
  'detailed-stock-pattern'."
  [line]
  (tt/regex-group-into-map line simple-stock-structure detailed-stock-pattern) )

(defn name?
  "A predicate to check 'stock' name against the current solution."
  [stock]
  #(= stock (-> % :stock)))

(defn action?
  "A predicate to check the 'buy' or 'sell' action of a stock."
  [action]
  #(= action (-> % :action)) )

(defn price-higher?
  "If a stock price is higher than X."
  [min]
  #(< min (read-string (-> % :price)) ) )

(defn price-lower?
  "If a stock price is lower than X."
  [max]
  #(> max (read-string (-> % :price)) ) )

(defn name-action?
  "A predicate to check 'stock' name and 'action' against the current solution.
  This also works with: (all? (name?) (action?))"
  [stock action]
  #(and (= stock (-> % :stock)) (= action (-> % :action))) )

(defn name-action-every-pred?
  "A predicate to check 'stock' name and 'action' against the current solution,
  using 'every-pred'.
  DEPRECATED: use (all?) or (any?) or both instead"
  [stock action]
  (every-pred (name? stock) #(= action (-> % :action))) )

(def increment-with-stock-quanity
  "Destructures 'solution' and existing 'count', and adds the stock 'quantity'
   'count'."
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )
