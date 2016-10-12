# clash
A clojure project for quick interactive analysis of structured text files (ex: logs, csv, etc.), within
the REPL. Define a representative structure with matching regular expressions for the text, then load the 
file into memory. This is useful for identifying trends and gaining insight from smaller datasets (~ millions of rows),
before starting a time consuming Hadoop or Spark job. Pivot, frequency, and count/collect functions have flags
for single/multi threaded executions.

Clash makes it fast and easy to determine how many times a specific value exists or identify common data fields
across millions of rows of data. This includes performance macros to determine approximately when the JVM will
optimize execution for a target method (sweetspot).

Try adding **[clash "1.2.3"]** to your project today

[transformation functions](#core-transformations)
[pivot functionality](#pivot)
[utility functions](#utility-functions)
[performance & debugging](#performance-debugging)
[general performance](#performance)
[packaged examples](#examples)
[setup](#setup)
[license](#license)

## Simple Usage
Convert millions of lines, from text stream or file, like this:
```
05042013-13:24:13.005|sample-server|1.0.0|info|Search,ZOO,25,13.99
```
into
```clojure
{:time "05042013-13:24:13.005", :action "Search", :name "ZOO", :quantity "25", :unit_price "13.99"}
```

## Core Transformation Functions
<a name="core-transformations"/>
Load data structures into memory and analyze or build result sets with predicates.

```clojure
; The 'parser' maps a structure onto lines of text (see Example below)
(transform-lines input parser :max xx :tdfx some-xform)

; Uses reduce, tracks counts and failed line parsings
(transform-lines-verbose filename parser :max xx)

; Slower, but atomic loads and useful when encountering errors
(atomic-list-from-file filename parser)

; Analyze and filter data with defined predicates
(count-with solutions predicates)
(count-with solutions predicatse :incrf + :initv 10 :plevel 2)

; Build a result set with via filters, etc for each 'solution'
(collect-with solutions predicates)
(collect-with solutions predicates :plevel 2)
```

## Utility Functions
<a name="utility-functions" />
Potentially useful functions to help filter and sort data. The resulting function will execute 
predicates from left to right. These are helpful for counting or collecting data that satisfy 
predicates.

#### Dictionary value frequencies
```clojure
; How many times do values repeat for specific keys across a collection of maps?
(collect-value-frequencies [{:a "a1" :b "b1"} {:a "a2" :b "b2"} {:a "a2" :c "c1"}])
=> {:c {c1 1} :b {b2 1, b1 1}, :a {a2 2, a1 1}}

; Concurrently get ':a' frequency values
(collect-value-frequencies mvs :kset [:a] :plevel 2)
=> {:a {a2 2, a1 1}} 

; Pass a function in for nested collections of maps
(def m1 {:a "a1" :b {:c [{:d "d1"} {:d "d1"} {:d "d2"}]}})
(collect-value-frequencies-for [m1] #(get-in % [:b :c]))
=> {:d {"d2" 1 "d1" 2}}

; Sort inner key values (descending)
(sort-value-frequencies {:a {"a1" 2 "a2" 5 "a3" 1}})
=> {:a {"a2" 5 "a1" 2 "a3" 1}}
```
#### Dictionary/List distinctness
```clojure
; Find distinct values for a given ~equality function for maps/lists
(distinct-by [{:a 1, :b 2} {:a 1, :b 3} {:a 2, :b 4}] #(-> % :a))
=> ({:a 2 :b 4} {:a 1 :b 2})
```

#### Predicate evaluations
```clojure
; Returns 'true', resembles (every-pred) and (some-fn), but perhaps more readable?
((all? number? even?) 10)
((any? number? even?) 11)
((none? odd?) 10)
(until? number? '("foo" 2 "bar"))
=> true
```

## Pivot & Pivot Matrix Functions
<a name="pivot"/>
This generates a list of predicate function groups (partials) that are applied to a collection of data (single or 
multi-threaded). Each predicate group is the result of a cartesian product from partial functions and their 
corresponding values (see example below). This results in a map that contains the count for each predicate group 
'match' in descending order.

The predicate functions should be contextually relevant for the collection of data (e.g. don't use numeric predicates 
with a list of strings).

Since it is hard for the JVM to keep the collections for large collections and predicate groups, only the count and 
underlying function are returned. Invidual result sets for any of the predicate groups may be obtained with 
(get-rs-from-matrix)

For example a collection 1 - 100,000:

1. Identify how many are divisible-by? 2, 3, 4, 5, 6, etc
2. Identify how many are also even?
3. Get the values from the collection for even? and (divisible-by? 5)

#### Pivot function
Simple use case for generating higher order functions from a base function and collection of arguments.
```clojure
; Use a vector instead of a list for r/fold parallelism
(pivot col msg :b common_pred? :p pivot_pred? :v pivot_values :plevel 2)

; Does a word contain a sub-string?
(defn str-inc? [c] #(clojure.string/includes? % c))

; Which of these words have these characters?
(pivot ["foo" "bar" "" bam"] "inc?" :b [#(not (empty? %))] :p inc? :v ["a" "b" "c"])

; Yields the following
{"inc?_[b]" 2, "inc?_[a]" 2, "inc?_[c]" 0}
```

#### Pivot Matrix function
Is similar to (pivot), but allows for multiple predicate functions and values by creating a cartesian function
group for all possible predicate combinations. Setting ':plevel 2' will utilize all cpu cores. (pivot-matrix*)
will build a result that includes: predicate key name, predicate function group, and the count of 'true' results
for a data set. The predicate function group may also be used to retrieve that specific subset of data.
```clojure
; combfx? -> all?, any?, none? (defaults to all?) 
(pivot-matrix* col msg :basefx? commonpred? :combfx? all? :pivots pivots :plevel 2)

;; Generate predicate groups (where combfx? --> all?) 
; --> (all? number? even? (divisible-by? 2))
; --> (all? number? even? (divisible-by? 3))
; --> (all? number? even? (divisible-by? 4))
(pivot-matrix* (range 1 100) "r100" :basefx? [number? even?] :pivots [{:f divisible-by? :v (range 2 5}])

; Where ':function' can be used to retrieve the result set
{"r1_[2]" {:count 49 :function #object[]}, 
 "r1_[4]" {:count 24 :function #object[]}, 
 "r1_[3]" {:count 16 :function #object[]}}

;; Generate a cartesian product combination of predicate groups:
; --> (all? number? even? (divisible-by? 2) (divisible-by? 6))
; --> (all? number? even? (divisible-by? 2) (divisible-by? 7)) 
; --> (all? number? even? (divisible-by? 3) (divisible-by? 6))
; --> (all? number? even? (divisible-by? 3) (divisible-by? 7)) 
; --> (all? number? even? (divisible-by? 4) (divisible-by? 6))
; --> (all? number? even? (divisible-by? 4) (divisible-by? 7)) 
(def even-numbers? [number? even?])
(pivot-matrix* (range 1 100) "r2" :basefx? even-numbers? :pivots [{:f divisible-by? :v (range 2 5)}
                                                                  {:f divisible-by? :v (range 6 8)]) 
                      
; The generated function is now included
(print-pivot-matrix pm)
("key: r2_[3|6], count: 16", 
 "key: r2_[2|6], count: 16",  
 "key: r2_[4|6], count: 8",  
 "key: r2_[2|7], count: 7",  
 "key: r2_[4|7], count: 3",  
 "key: r2_[3|7], count: 2")
```

#### Pivot utility functions
Printing, comparing, and retrieving interesting subsets of data from the (pivot-matrix*) result.

```clojure
; Get a result set for any of the predicate groups in a matrix
(def mtrx (pivot-matrix* (range 1 100) "foo" :basefx? [even?] :pivots [{:f divisible-by? :v (range 2 6)}]) 
                                                      
(pprint mtrx) 
{"foo_[2]" {:count 49},
 "foo_[3]" {:count 24},
 "foo_[4]" {:count 16},
 "foo_[5]" {:count 9}}

; All of the even numbers divisible by 5 for 1 - 100
(pivot-rs hundred mtrx "foo_[5]")
user=> (90 80 70 60 50 40 30 20 10)

; Filter for specific pivots (could do w/o :kterms below)
(filter-pivots hundred :kterms ["4" "3"] :cfx even?)
user=> {"foo_[3]" {:count 24} "foo_[4]" {:count 16}}

; Compare 2 collections of similar data and compare with function (ratio)
(def c1 (range 1 50))
(def c2 (range 50 120))
(pivot-matrix-compare c1 c2 "foo" ratio :b [number?] :p [divisible-by?] :v [(range 2 6)])

; Find results where the count > 20
(filter-pivots mtrx :cfx #(> % 20))

; Find results where key name contains "3"
(filter-pivots mtrx :kterms ["3"])
```

## Performance & Debug Utilities
<a name="performance-debugging"/>
These utility functions are useful for determining performance profiles for a given function within the JVM. This is helpful
when identifying how many function executions before the JVM optimizes itself for code execution. For example, simple addition
is optimized in less than 20 executions, while a regular expression might take 100+ executions before realizing best performance.

```clojure
=> (perf (+ 3 3))
Time(ns): 168
6

; Evaluate function performance (debug, etc)
=> (perfd (+ 3 3)
debug value: 6, Time(ns): 2100

; By default, capture of function values will not occur
=> (repeatfx 5 (+ 4 4) :capture true)
{:total_time 6832, :values [8 8 8 8 8], :average_time 1366.4}

; What is the Hotspot performance curve for a function.
; Use ':verbose true' to see System/heap stats
=> (sweetspot (clojure.string/split "This is a test" #" "))
{:system {},
 :total 94774,
 :count 3,
 :results
 [{:n 10, :average_time 3847.4}
  {:n 20, :average_time 1133.45}
  {:n 30, :average_time 1121.033}]}
  
; To format time, use the elapsed function
=> (elapsed (:average_time (repeatfx 5 (+ 4 4))))
Time(ns): 1366.4  
```


## Packaged Examples
<a name="examples"/>
1. src/clash/example/web_shop_example.clj
2. test/clash/example/web_shop_example_test.clj
3. test/resources/web-shop.log

#### define object structure, regex, and parser for sample text
```clojure
; time|application|version|logging_level|log_message (Action, Name, Quantity, Unit Price)
(def line "05042013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00") 
 
; Defrecords offer a 12%-15% performance improvement during parsing vs maps
(def simple-structure [:time :action :name :quantity :unit_price])

; 10022013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00
(def detailed-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn into-memory-parser
  "An exact parsing of line text into 'simple-structure' using
  'detailed-pattern'."
  [text_line]
  (tt/regex-group-into-map text_line simple-structure detailed-pattern) )

; Create a dataset from raw text file to work with   
(def solutions (file-into-structure web-log-file into-memory-parser []))

; Save dataset as .edn for future access (slower than reparsing the original text)
(data-to-file solutions "/some/local/directory/solutions")    
(data-from-file "/some/local/directory/solutions.edn")    
```

#### sample log data (web-shop.log) 
```
# time|application|version|logging_level|log_message (Action, Name, Quantity, Unit Price)
05042013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00
05042013-13:24:12.010|sample-server|1.0.0|info|Search,FOO,2,15.00
05042013-13:24:12.130|sample-server|1.0.0|info|Search,BAR,10,2.25
05042013-13:24:12.130|sample-server|1.0.0|info|NullPointerException: not enough defense programming
05042013-13:24:12.450|sample-server|1.0.0|info|Price,FOO,2,15.00
05042013-13:24:12.750|sample-server|1.0.0|info|Price,BAR,10,2.25
05042013-13:24:13.005|sample-server|1.0.0|info|Search,ZOO,25,13.99
05042013-13:24:13.123|sample-server|1.0.0|info|Purchase,BAR,10,2.25
```

#### Load the examples
```
lein repl
```
```clojure
; With exact parser (and regex) - reads specific lines
user=> (def sols (transform-lines web-log-file weblog-parser))
#'user/sols
user=> (count sols)
7

user=> (first sols)
{:time "05042013-13:24:12.000", :action "Search", :name "FOO", :quantity "5", :unit_price "15.00"}

; Create a larger fileset from weblog
user=> (def weblog load-weblog)
user=> (def weblog_40k (grow-weblog 5000 weblog))
user=> (lines-to-file "larger-40k-shop.log" weblog_40k)
```

#### Single conditions and incrementing functions
```clojure
; in the context of a map composed of 'structure' keys
(defn name-action?
  "A predicate to check :name and :action against the current solution.
  This also works with: (all? (name?) (action?))"
  [name action]
  (fn [line] (and (= name (-> line :name)) (= action (-> line :action)))) )

; A count of all "Search" actions for "FOO"
user=> (count-with solutions (name-action? "FOO" "Search"))
2    

; A running total of a specific key field  
(def increment-with-quanity
  "Increments a count based on the :quantity for each solution in the collection"
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )  
    
; Incrementing count based on quantity for each structure
user=> (count-with sols (name? "FOO") :incrf increment-with-quanity)
9

; Collecting a sequence of all matching solutions
user=> (count (collect-with sols (name-action? "BAR" "Purchase")))
1
```

#### Using multiple conditions (all?) and (any?) to filter data
```clojure
(defn price-higher?
  "If the unit price is higher than X."
  [min]
  (fn [line] (< min (read-string (-> line :unit_price)))) )

(defn price-lower?
  "If the unit price is lower than X."
  [max]
  (fn [line] (> max (read-string (-> line :unit_price)))) )

; Using (all?)
user=> (count-with @sols (all? (price-higher? 12.10) (price-lower? 14.50) ) )
1

; Using (all?) and (any?) together
user=> (count-with @sols (all? (any? (price-higher? 12.20) (price-lower? 16.20)) ) )
4
```

#### Alternatively creating maps from key sets and regex groups
Some basic internal utility for creating a map composed of structured keys and
a regular expression.
```clojure
;; mapping regex groups: text, structure, pattern, sub-key(s) into a list of maps
    
; Return all keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)")
=> ({:a "a" :b "b"} {:a "c" :b "d"})    
    
; Only return ':a' keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)" [:a])
=> ({:a "a"} {:a "c"})
```
## Shell Command Interaction
Applying linux/unix shell commands in conjunction with Clojure to a text file. It's
generally faster to delegate to the C implementations than iterate through a file
with the JVM. These are simple, included test files.
```clojure
(def command2 (str "grep message " input1 " | cut -d\",\" -f2 " input1))
(def output2 (str tresource "/output2.txt"))

; Writes result to output2 (see test/command.clj)
(jproc-write command2 output2 ":")
```

```clojure
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

; Writes result to output1 (see test/command.clj)
(jproc-write command1 output1 "\n")
```
#### Shell notes
A simple performance test comparing '(re-split (re-find))' vs '(jproc-write "grep | cut")' and a
145 MB file resulted in completed in less than half the time with (jproc-write).

A performance macro that will adjust the time unit for better readability and context. It will print 
elapsed time in nano seconds (ns), milliseconds (ms) or seconds(s). 
```clojure
; function that returns {:result :latency_text}
(latency exe message)

; macro that returns result and prints latency
(perf expr message)
    
(def message2 "'cl + grep + cut'")
(perf (jproc-write command2 output2 ":") message) --> 'cl + grep + cut' Time(ms):18.450
```
## Setup
<a name="setup" />
1. Retrieve code for stand alone use or as a resource
    * git clone
    * git submodule add  <your-project>/checkouts
2. Install Leiningen and update the **project.clj**
    * Adjust based on number and complexity of structured objects

### notes
<a name="notes" />
* requires "/bin/sh" functionality
* works best with java 1.8
* built with leiningen (thanks technomancy)
* clojure 1.8 (thank rich, et al)

<a name="performance"/>
* Log/text files with 5+ million lines
* Most (count), (collect), and (pivot) take <= 1s per million rows*
  * simple data 5 key-values might evaluate at 20+ million rows/s
  * up to 2 - 3 million rows (30 elements, 2 nest levels) evaluated per second*
* 95,000 similar maps with 8 keys each in ~0.6 seconds**
* 400,000 filter groups and 560,000 complex data rows stable for 9 hours and < 4 gb JVM Heap**

\*Macbook Pro
\**old 4 core pentium 4 with 8 gigs of RAM

## License
<a name="license" />
Copyright (c) David Millett 2012. All rights reserved.
The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

