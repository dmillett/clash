# clash
A clojure project for quick interactive analysis of structured text files (ex: logs, csv, etc.), within
the REPL. Define a representative structure with matching regular expressions for the text, then load the 
file into memory. This is useful for identifying trends and gaining insight from smaller datasets (~ millions of rows),
before starting a time consuming Hadoop or Spark job. Pivot, frequency, and count/collect functions have flags
for single/multi threaded executions.

Clash makes it fast and easy to determine how many times a specific value exists or identify common data fields
across millions of rows of data. This includes performance macros to determine approximately when the JVM will
optimize execution for a target method (sweetspot).

Try adding **[clash "1.5.0"]** to your project today

 * [transformation functions](#core-transformations)
 * [pivot functionality](#pivot)
 * [haystack functionality](#haystack)
 * [data shape](#data-shape)
 * [utility functions](#utility-functions)
 * [performance & debugging](#performance-debugging)
 * [general performance](#performance)
 * [packaged examples](#packaged-examples)
 * [deprecated](#deprecated)
 * [setup](#setup)
 * [license](#license)

## Simple Usage
Convert, into memory, millions of lines, from text/csv/json/etc stream or file, like this:
```
05042013-13:24:13.005|sample-server|1.0.0|info|Search,ZOO,25,13.99
```
using
```clojure
;; Create a target structure, pattern, and parser/adapter
(defrecord Structure [time action name quantity unit_price])
(def pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

;; Could use split for each line of CSV or cheshire to read/parse each line of JSON
(defn parser [line] (if-let [[_ t a n q p] (re-find pattern line)] (->Structure t a n q p)))

;; Parse and transform a single line
(def sample "05042013-13:24:13.005|sample-server|1.0.0|info|Search,ZOO,25,13.99")
(parser sample)
```
into
```clojure
;; Transform one million log lines (2 - 3 seconds on macbook)
(def data (transform-lines "logs.txt" parser :max 1000000))

;; Single line sample
#user.Structure{:time "05042013-13:24:13.005", :action "Search", :name "ZOO", :quantity "25", :unit_price "13.99"}
```

<a name="core-transformations"/></a>
## Core Transformation Functions
Load data structures into memory and analyze or build result sets with predicates.

```clojure
;; Using a transducer function, the 'parser' maps a structure onto lines of content (see 'packaged examples' below)
;; Additional arguments :joinfx (default: conj) and :initv (default: []) are also available. 
(transform-lines input parser :max xx :tdfx some-xform)

;; Using a reducer function, tracks counts and failed line parsings
(transform-lines-verbose filename parser :max xx)

;; Slower, but atomic loads and useful when encountering errors
(atomic-list-from-file filename parser)

;; Pre-process or extract subsets of large JSON or XML data without unnecessary parsing
;; For large JSON regex groups, this is much faster than 'awk' or 'jq'
(disect istream ostream :fx (regex-magic))
```

<a name="haystack"/></a>
## Haystack Functionality
Combines (collect-value-frequencies) and (pivot-matrix) to correlate the frequency of specific data schema keys across
a collection of data. For example, find the 5 most frequent values for specific keys (like :price :quantity), and let
(haystack) figure out which data rows in a collection have the 'most frequent' values. The result indicates how many 
data rows satisfied the constraints and provides the corresponding function to retrieve that result set from the 
collection (see 'pivot-rs).

**For example, find out when purchases with the largest markup happened?**
```clojure
;; A tiny subset of mock purchase data
(def purchases
[{:name "foo" :time {:hour 1 :minute 10 :second 20} :price {:markup 0.10 :base 1.00 :tax 0.05}}
{:name "bar" :time {:hour 1 :minute 10 :second 50} :price {:markup 0.07 :base 1.15 :tax 0.06}}
{:name "foo" :time {:hour 1 :minute 10 :second 52} :price {:markup 0.10 :base 1.00 :tax 0.05}}
{:name "foo" :time {:hour 1 :minute 12 :second 14} :price {:markup 0.12 :base 1.00 :tax 0.05}}
])

;; The top 'n' values for a schema path/set 
(defn top-freqs [n] (partial reduce-vfreqs #(take n (sort-map-by-value %))))

;; The bottom 'n' values for a schema path/set
(defn bottom-freqs [n] (partial reduce-vfreqs #(take n (sort-map-by-value % :descending false))))

;; Grab the most frequent schema values for ':time' and ':price' schema paths
;; Use ':plevel 2' (all cores) for larger datasets
(def hstack (haystack purchases :vffx (top-freqs 1) :vfkpsets [{:kp [:time] :ks [:hour :minute]} 
                                                               {:kp [:price] :ks [:markup]}]))

;; Count, when true, for the schema & value combinations ([path 1]|[path n]_[value 1]|[value n])
(pprint hstack)
{"haystack([:time :hour]|[:time :minute]|[:price :markup])_[1|10|0.1]"
 {:count 2,
  :function
  #object[clash.tools$all_QMARK_$fn__3155 0x6137ad7c "clash.tools$all_QMARK_$fn__3155@6137ad7c"]}}
  
;; Get the result set for those criteria
(pivot-rs purchase hstack "haystack([:time :hour]|[:time :minute]|[:price :markup])_[1|10|0.1]")
({:name "foo", :time {:hour 1, :minute 10, :second 20}, :price {:markup 0.1, :base 1.0, :tax 0.05}} 
{:name "foo", :time {:hour 1, :minute 10, :second 52}, :price {:markup 0.1, :base 1.0, :tax 0.05}})  
```

*haystack generates the functions for schema key paths and evaluates those against generic/specific constraints*

### Correlation for a subset of schema keys 
```clojure
;; Find for key paths [:a :b] and [:a :c] ... ignore schema path [:a :d]
(def data [{:a {:b 1 :c 2 :d 3}} {:a {:b 1 :c 3 :d 3}} {:a {:b 2 :c 3 :d 3}}])

;; Generates functions to evaluate 4 value combinations for all possible values
(def hstack1 (haystack data :vfkpsets [{:kp [:a] :ks [:b :c]}]))

;; vf([key-path 1]|[key-path 2]|[key-path N])
;; Note that none of the data has a nested map for {:a {:b 2 :c2}}, --> :count 0
(pprint hstack1)
{"haystack([:a :b]|[:a :c])_[2|3]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0xcd6fdab "clash.tools$all_QMARK_$fn__1532@cd6fdab"]},
 "haystack([:a :b]|[:a :c])_[1|3]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x779f98a2 "clash.tools$all_QMARK_$fn__1532@779f98a2"]},
 "haystack([:a :b]|[:a :c])_[1|2]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x5efae983 "clash.tools$all_QMARK_$fn__1532@5efae983"]},
 "haystack([:a :b]|[:a :c])_[2|2]"
 {:count 0,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x5c789083 "clash.tools$all_QMARK_$fn__1532@5c789083"]}}
```

### Find the rows based on the most frequent value for a schema key

```clojure
;; Top 1 (or N) occuring values for any data schema key (t -> clash.tools)
(defn top [n] (partial t/reduce-vfreqs #(take n (t/sort-map-by-value %)))

(haystack data :vfkpsets [{:kp [:a]}] :vffx (top 1))
{"haystack([:a :b]|[:a :c]|[:a :d])_[1|3|3]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x31c221a7 "clash.tools$all_QMARK_$fn__1532@31c221a7"]}}

;; Get the data row(s) result set that satisfies the constraints
(pivot-rs data h1 "haystack([:a :b]|[:a :c]|[:a :d])_[1|3|3]")
({:a {:b 1, :c 3, :d 3}})
```

### Correlation with existing pivot function and schema key paths

```clojure
;; Does any data row have a {:a {:d _}} that is evenly divisible by [3, 4]? 
(defn dmod? [n] #(try (zero? (mod (get-in % [:a :d]) n)) (catch NullPointerException _ false)))

(def data [{:a {:b 1 :c 2}} {:a {:b 1 :c 3}} {:a {:b 2 :c 3}}])

;; Check if any nested map for [:a] has a [:d] value divisible by 3? And check for [:b, :c] paths
(haystack data :msg "d3_[dmod]" :kpath [:a] :kset [:b :c] :pivots [{:f dmod? :v [3]}])

{"d3_[dmod]_vf([:a :b]|[:a :c])_[[3]|2|3]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x6ba96652 "clash.tools$all_QMARK_$fn__1532@6ba96652"]},
 "d3_[dmod]_vf([:a :b]|[:a :c])_[[3]|1|3]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x3d2354dc "clash.tools$all_QMARK_$fn__1532@3d2354dc"]},
 "d3_[dmod]_vf([:a :b]|[:a :c])_[[3]|1|2]"
 {:count 1,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x194f4ea4 "clash.tools$all_QMARK_$fn__1532@194f4ea4"]},
 "d3_[dmod]_vf([:a :b]|[:a :c])_[[3]|2|2]"
 {:count 0,
  :function
  #object[clash.tools$all_QMARK_$fn__1532 0x3a16e450 "clash.tools$all_QMARK_$fn__1532@3a16e450"]}}
```

<a name="pivot"/></a>
## Pivot & Pivot Matrix Functions
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

### Pivot function
Simple use case for generating higher order functions from a base function and collection of arguments.
```clojure
;; Use a vector instead of a list for r/fold parallelism
(pivot col msg :b common_pred? :p pivot_pred? :v pivot_values :plevel 2)

;; Does a word contain a sub-string?
(defn str-inc? [c] #(clojure.string/includes? % c))

;; Which of these words have these characters?
(pivot ["foo" "bar" "" bam"] "inc?" :b [#(not (empty? %))] :p inc? :v ["a" "b" "c"])

;; Yields the following
{"inc?_[b]" 2, "inc?_[a]" 2, "inc?_[c]" 0}
```
### Pivot Matrix function
Is similar to (pivot), but allows for multiple predicate functions and values by creating a cartesian function
group for all possible predicate combinations. Setting ':plevel 2' will utilize all cpu cores. (pivot-matrix*)
will build a result that includes: predicate key name, predicate function group, and the count of 'true' results
for a data set. The predicate function group may also be used to retrieve that specific subset of data.
```clojure
;; combfx? -> all?, any?, none? (defaults to all?) 
(pivot-matrix* col msg :basefx? commonpred? :combfx? all? :pivots pivots :plevel 2)

;; Generate predicate groups (where combfx? --> all?) 
; --> (all? number? even? (divisible-by? 2))
; --> (all? number? even? (divisible-by? 3))
; --> (all? number? even? (divisible-by? 4))
(pivot-matrix* (range 1 100) "r100" :basefx? [number? even?] :pivots [{:f divisible-by? :v (range 2 5}])

;; Where ':function' can be used to retrieve the result set
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
                      
;; The generated function is now included
(print-pivot-matrix pm)
("key: r2_[3|6], count: 16", 
 "key: r2_[2|6], count: 16",  
 "key: r2_[4|6], count: 8",  
 "key: r2_[2|7], count: 7",  
 "key: r2_[4|7], count: 3",  
 "key: r2_[3|7], count: 2")
```
### Pivot utility functions
Printing, comparing, and retrieving interesting subsets of data from the (pivot-matrix*) result.

```clojure
;; Get a result set for any of the predicate groups in a matrix
(def mtrx (pivot-matrix* (range 1 100) "foo" :basefx? [even?] :pivots [{:f divisible-by? :v (range 2 6)}]) 
                                                      
(pprint mtrx) 
{"foo_[2]" {:count 49},
 "foo_[3]" {:count 24},
 "foo_[4]" {:count 16},
 "foo_[5]" {:count 9}}

;; All of the even numbers divisible by 5 for 1 - 100
(pivot-rs hundred mtrx "foo_[5]")
(90 80 70 60 50 40 30 20 10)

;; Filter for specific pivots (could do w/o :kterms below)
(filter-pivots hundred :kterms ["4" "3"] :cfx even?)
{"foo_[3]" {:count 24} "foo_[4]" {:count 16}}

;; Compare 2 collections of similar data and compare with function (ratio)
(def c1 (range 1 50))
(def c2 (range 50 120))
(pivot-matrix-compare c1 c2 "foo" ratio :b [number?] :p [divisible-by?] :v [(range 2 6)])

;; Find results where the count > 20
(filter-pivots mtrx :cfx #(> % 20))

;; Find results where key name contains "3"
(filter-pivots mtrx :kterms ["3"])
```

<a name="data-shape"/></a>
## Data Shape
When encountering a lot of JSON and/or XML data of unknown structure, it is helpful to flatten each
structure into a single depth map the captures which structures and values occur the most. Some nested
fields might occur a few times per thousand records and be prioritized lower.

### JSON
These four JSON data are similar, but not identical in structure:
```json
{"a":1, "b":[{"c":2, "d":3},{"c":4, "d":5},{"c":6, "d":7}]},
{"a":1, "b":{"c":3, "d":4}}
{"a":1, "b":[{"c":2, "d":{"e":[8,9]}},{"c":4, "d":{"e":[10,11]}},{"c":6, "d":{"f":true}}]},
{"a":1, "b":2, "c":[3,4,5]}
```

The new keys represent the path to the original structure ('.' delimited). Both Cheshire and clojure.data.json
will work for parsing JSON text, but Cheshire seems to be faster so it's the default. 

```clojure
(def flattened (apply merge-data (map #(flatten-json %) [json1 json2 json3 json4])))
(pprint flattened)
{"a" [1 1 1 1], "b" [2], "c" [3 4 5], "b.c" [3 2 4 6 2 4 6], "b.d" [4 3 5 7], "b.d.e" [8 9 10 11], "b.d.f" [true]}

(flat-data-value-counts flattened)
{"a" 4, "b" 1, "c" 3, "b.c" 7, "b.d" 4, "b.d.e" 4, "b.d.f" 1}
```

#### Load a file or text stream & determine value shape**

Define regular expression patterns that the values might represent. For example: int, decimal, text, boolean, etc by
including an ordered list of `(->ValuePattern :type #"some-pattern" (fx [v] (pre-process v)))`

```clojure
;; See (simple-json-parser) and (keypath-frequencies) in clash.shape.clj
(def flat (transform-lines "<input-file-name>" simple-json-parser :joinfx keypath-frequencies :initv {}))

{"a" 4, "b" 1, "c" 1, "b.c" 3, "b.d" 2, "b.d.e" 1, "b.d.f" 1}

;; See (simple_patterns) and (keypath-value-patterns) in clash.shape.clj 
(def freqs (transform-lines input simple-json-parser :joinfx (partial keypath-value-patterns simple_patterns) :initv {}))
(def shaped (shape-sort freqs))

{"b.c" {:int 4, :financial 3}, "b.d.e" {:int 4}, "b.d" {:int 3, :decimal 1}, "a" {:int 4}, "c" {:int 3}, "b.d.f" {:boolean 1}, "b" {:decimal 1}}
```

### XML

```clojure
(def xml1 "<A a=\"a1\" ax=\"ax1\"><B><C c=\"c1\"/><C c=\"c2\">foo</C><C>bar</C></B><D>zoo</D><E><F f=\"f1\">cats</F><F f=\"f1\"/></E></A>")
(flatten-xml xml1)

;; Yields
{"A.@ax" ["ax1"], "A.@a" ["a1"], "A.B.C.@c" ["c1" "c2"], "A.B.C" ["foo" "bar"], "A.D" ["zoo"], "A.E.F.@f" ["f1" "f1"], "A.E.F" ["cats"]}
(flatten-data-frequencies xmlflat1)
{"A.@ax" 1, "A.@a" 1, "A.B.C.@c" 2, "A.B.C" 2, "A.D" 1, "A.E.F.@f" 2, "A.E.F" 1}
```

<a name="utility-functions"/></a>
## Utility Functions
Potentially useful functions to help filter and sort data. The resulting function will execute 
predicates from left to right. These are helpful for counting or collecting data that satisfy 
predicates.

### Dictionary value frequencies
```clojure
;; Sample of mock purchase data
(def purchases 
  [{:name "foo" :time {:hour 1 :minute 10 :second 20} :price {:markup 0.10 :base 1.00 :tax 0.05}}
   {:name "bar" :time {:hour 1 :minute 10 :second 50} :price {:markup 0.07 :base 1.15 :tax 0.06}}
   {:name "foo" :time {:hour 1 :minute 10 :second 52} :price {:markup 0.10 :base 1.00 :tax 0.05}}
   {:name "foo" :time {:hour 1 :minute 12 :second 14} :price {:markup 0.12 :base 1.00 :tax 0.05}}
  ])

;; Find value frequencies for ':name
(mv-freqs purchases :kpsets [{:ks [:name]}])
{:name {"foo" 3, "bar" 1}}

;; Find value frequencies for different schema paths and fields
(mv-freqs purchases :kpsets [{:ks [:name]} 
                             {:kp [:time] :ks [:hour :minute]} 
                             {:kp [:price] :ks [:base :tax]}])
                             
{:name {"foo" 3, "bar" 1}, 
 :hour {1 4}, 
 :minute {10 3, 12 1}, 
 :base {1.0 3, 1.15 1}, 
 :tax {0.05 3, 0.06 1}}

;; Sort inner key values (descending)
(sort-value-frequencies {:a {"a1" 2 "a2" 5 "a3" 1}})
{:a {"a2" 5 "a1" 2 "a3" 1}}

;; Filter frequencies by keys, values, or both
(filter-value-frequencies vfreqs (fn [[_ v]] (even? v)))
{:a {"a1" 2}}
```
### Dictionary/List distinctness
```clojure
;; Distinct values using a function for maps/lists
(distinct-by [{:a 1, :b 2} {:a 1, :b 3} {:a 2, :b 4}] #(:a %))
({:a 2 :b 4} {:a 1 :b 2})
```
### Predicate evaluations
```clojure
;; Returns 'true', resembles (every-pred) and (some-fn), but perhaps more readable?
((all? number? even?) 10)
((any? number? even?) 11)
((none? odd?) 10)
(until? number? '("foo" 2 "bar"))
true
```
### Count & collect data (1 or all threads)
Using one or multiple threads (:plevel 2), count or collect data based on specified 
predicates.
```clojure
;; Count with predicates and incrementing function
(count-with solutions predicates)
(count-with solutions predicatse :incrf + :initv 10 :plevel 2)

;; Create a result set with predicates
(collect-with solutions predicates)
(collect-with solutions predicates :plevel 2)
```

<a name="performance-debugging"/></a>
## Performance & Debug Utilities
These utility functions are useful for determining performance profiles for a given function within the JVM. This is helpful
when identifying how many function executions before the JVM optimizes itself for code execution. For example, simple addition
is optimized in less than 20 executions, while a regular expression might take 100+ executions before realizing best performance.

```clojure
(perf (+ 3 3))
Time(ns): 168
6

; ;Evaluate function performance (debug, etc)
(perfd (+ 3 3)
debug value: 6, Time(ns): 2100

;; By default, capture of function values will not occur
(repeatfx 5 (+ 4 4) :capture true)
{:total_time 6832, :values [8 8 8 8 8], :average_time 1366.4}

;; What is the Hotspot performance curve for a function.
;; Use ':verbose true' to see System/heap stats
(sweetspot (clojure.string/split "This is a test" #" "))
{:system {},
 :total 94774,
 :count 3,
 :results
 [{:n 10, :average_time 3847.4}
  {:n 20, :average_time 1133.45}
  {:n 30, :average_time 1121.033}]}
  
;; To format time, use the elapsed function
(elapsed (:average_time (repeatfx 5 (+ 4 4))))
Time(ns): 1366.4  
```

<a name="packaged-examples"/></a>
## Packaged Examples
1. src/clash/example/web_shop_example.clj
2. test/clash/example/web_shop_example_test.clj
3. test/resources/web-shop.log

#### define object structure, regex, and parser for sample text
```clojure
;; time|application|version|logging_level|log_message (Action, Name, Quantity, Unit Price)
(def line "05042013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00") 
 
;; Defrecords offer a 12%-15% performance improvement during parsing vs maps
(def simple-structure [:time :action :name :quantity :unit_price])

;; 10022013-13:24:12.000|sample-server|1.0.0|info|Search,FOO,5,15.00
(def detailed-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

(defn into-memory-parser
  "An exact parsing of line text into 'simple-structure' using
  'detailed-pattern'."
  [text_line]
  (tt/regex-group-into-map text_line simple-structure detailed-pattern) )

;; Create a dataset from raw text file to work with   
(def solutions (file-into-structure web-log-file into-memory-parser []))

;; Save dataset as .edn for future access (slower than reparsing the original text)
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
;; With exact parser (and regex) - reads specific lines
(def sols (transform-lines web-log-file weblog-parser))
(count sols)
7

(first sols)
{:time "05042013-13:24:12.000", :action "Search", :name "FOO", :quantity "5", :unit_price "15.00"}

;; Create a larger fileset from weblog
(def weblog load-weblog)
(def weblog_40k (grow-weblog 5000 weblog))
(lines-to-file "larger-40k-shop.log" weblog_40k)
```

#### Single conditions and incrementing functions
```clojure
;; in the context of a map composed of 'structure' keys
(defn name-action?
  "A predicate to check :name and :action against the current solution.
  This also works with: (all? (name?) (action?))"
  [name action]
  (fn [line] (and (= name (-> line :name)) (= action (-> line :action)))) )

;; A count of all "Search" actions for "FOO"
(count-with solutions (name-action? "FOO" "Search"))
2    

;; A running total of a specific key field  
(def increment-with-quanity
  "Increments a count based on the :quantity for each solution in the collection"
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )  
    
; Incrementing count based on quantity for each structure
(count-with sols (name? "FOO") :incrf increment-with-quanity)
9

;; Collecting a sequence of all matching solutions
(count (collect-with sols (name-action? "BAR" "Purchase")))
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

;; Using (all?)
(count-with @sols (all? (price-higher? 12.10) (price-lower? 14.50) ) )
1

;; Using (all?) and (any?) together
(count-with @sols (all? (any? (price-higher? 12.20) (price-lower? 16.20)) ) )
4
```

#### Alternatively creating maps from key sets and regex groups
Some basic internal utility for creating a map composed of structured keys and
a regular expression.
```clojure
;; mapping regex groups: text, structure, pattern, sub-key(s) into a list of maps
    
;; Return all keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)")
({:a "a" :b "b"} {:a "c" :b "d"})    
    
;; Only return ':a' keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)" [:a])
({:a "a"} {:a "c"})
```
## Shell Command Interaction
Applying linux/unix shell commands in conjunction with Clojure to a text file. It's
generally faster to delegate to the C implementations than iterate through a file
with the JVM. These are simple, included test files.
```clojure
(def command2 (str "grep message " input1 " | cut -d\",\" -f2 " input1))
(def output2 (str tresource "/output2.txt"))

;; Writes result to output2 (see test/command.clj)
(jproc-write command2 output2 ":")
```

```clojure
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

;; Writes result to output1 (see test/command.clj)
(jproc-write command1 output1 "\n")
```
#### Shell notes
A simple performance test comparing '(re-split (re-find))' vs '(jproc-write "grep | cut")' and a
145 MB file resulted in completed in less than half the time with (jproc-write).

A performance macro that will adjust the time unit for better readability and context. It will print 
elapsed time in nano seconds (ns), milliseconds (ms) or seconds(s). 
```clojure
;; function that returns {:result :latency_text}
(latency exe message)

;; macro that returns result and prints latency
(perf expr message)
    
(def message2 "'cl + grep + cut'")
(perf (jproc-write command2 output2 ":") message) --> 'cl + grep + cut' Time(ms):18.450
```

<a name="deprecated"/></a>
## Deprecated

### commits
```bash
6a19727 Removed deprecated (fold-conj) use (conj) and (str-contains?) use (clojure.string/includes?)
2017e49 Removed deprecated (get-rs-from-matrix) use (pivot-rs)
42525fe Removed deprecated (pivot-matrix-e) use (pivot-matrix*). Deprecated clash.core/atomic-*
d4af497 removed deprecated (atomic-map-from-file), use (transform-lines)
```

<a name="setup"/></a>
## Setup
1. Retrieve code for stand alone use or as a resource
    * git clone
    * git submodule add  <your-project>/checkouts
2. Install Leiningen and update the **project.clj**
    * Adjust based on number and complexity of structured objects

### General Notes
<a name="notes"/></a>
* requires "/bin/sh" functionality
* works best with java 1.8
* built with leiningen (thanks technomancy)
* clojure 1.8 (thank rich, et al)

<a name="performance"/></a>
* Log/text files with 5+ million lines
* Most (count), (collect), and (pivot) take <= 1s per million rows*
  * simple data 5 key-values might evaluate at 20+ million rows/s
  * up to 2 - 3 million rows (30 elements, 2 nest levels) evaluated per second*
* 95,000 similar maps with 8 keys each in ~0.6 seconds**
* 400,000 filter groups and 560,000 complex data rows stable for 9 hours and < 4 gb JVM Heap**

\*Macbook Pro
\**old 4 core pentium 4 with 8 gigs of RAM

<a name="license"/></a>
## License
Copyright (c) David Millett 2012. All rights reserved.
The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

