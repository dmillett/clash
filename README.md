# clash
A clojure project for quick interactive analysis of structured text files (ex: logs, csv, etc.), within
the REPL. After parsing structured text into an in memory data structure as an *atom*, clash facilitates
counting and collecting results from the data structure based on specified predicates and incrementing
functions. This is valuable for quickly analyzing data that is not persisted in databases.

Clash also includes clojure-shell (bash, csh, zsh, ec) functionality that incorporates most of the speed 
advantages of commands like 'grep' and 'cut' individually or piped together. The results of unpiped/piped
shell command are OS specific (non MS Windows), but offer better performance than pure clojure/java 
grep/cut implementations.

## Features & Benefits
1. Quickly load small-large log or text file into an object structure 
2. Very fast, condition based, result counts and retrievals 
3. Quickly build and analyze text data withing Clojure REPL
 * Existing log/text file data for patterns and ML
 * Experiment and identify optimal data queries for larger scale Hadoop style analysis
 * Determine initial trends

I am using this library as the basis of a more comprehensive library at Orbitz (my day job),
to chew on (pun intended), complex data structures dumped in log files.
* Log files with 40,000 - 500,000 entries
* Each solution entry contains multiple nested structures
* File load time into memory ranges from 1.0 - 25 seconds
* 30+ custom predicate and increment functions
* Most 'count' and 'collect' functions take 20 ms to 1.5 seconds
* Use **defrecord** offers 12-15% performance improvement over map

*old 4 core pentium 4 with 8 gigs of RAM*

## Usage
These examples can be found in in the example namespace of this repository.
### Core functions to build upon
Build on these functions with domain specific structure
```clojure
; Load objects from a file into memory (via defined regex and keyset)
; It's possible to transform text prior to parsing and apply predicates
(atomic-list-from-file filename parser)
(atomic-map-from-file filename parser)

; Build filters with conditionals
((all? predicate1? (any? predicate2? predicate3?) predicate4?) solution_data)

; Analyze data with defined predicates (filters with 'and'/'or' functionality)
; Incrementers can extract information and update cumulative results
; Count or total specific pieces of data per 'solution'
(count-with-conditions solutions predicates)
(count-with-conditions solutions predicates initial_count incrementer)

; Build a result set with via filters, etc for each 'solution'
(collect-with-conditions solutions predicates)

```
### Examples
1. src/clash/example/stock_example.clj
2. test/clash/example/stock_example_test.clj
3. test/resources/simple-structured.log

#### data 
```
# time|application|version|logging_level|log_message
05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00
05042013-13:24:12.010|sample-server|1.0.0|info|Buy,FOO,200,12.20
05042013-13:24:12.130|sample-server|1.0.0|info|Buy,BAR,1000,2.25
05042013-13:24:12.130|sample-server|1.0.0|info|NullPointerException: not enough defense programming
05042013-13:24:12.450|sample-server|1.0.0|info|Sell,FOO,500,12.72
05042013-13:24:13.005|sample-server|1.0.0|info|Buy,ZOO,200,9.24
05042013-13:24:13.123|sample-server|1.0.0|info|Sell,BAR,50,2.30
```
#### define object structure, regex, and parser for sample text
```clojure
;; A log line example from (simple-structured.log)
(def line "05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00")
 
; Defrecords offer a 12%-15% performance improvement during parsing
(def simple-stock-structure [:trade_time :action :stock :quantity :price])
(def detailed-stock-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

; Parse log line into 'simple-stock-structure' via 'detailed-stock-pattern'
(defn better-stock-message-parser
  "An exact parsing of line text into 'simple-stock-structure' using
  'detailed-stock-pattern'."
  [line]
  (tt/regex-group-into-map line simple-stock-structure detailed-stock-pattern) )
```
#### Load the examples
```
lein repl
```
```clojure
; First pass with a general, simpler parser (and regex) - reads every line
user=> (def sols (atomic-list-from-file simple-file simple-stock-message-parser))
#'user/sols
user=> (count @sols)
8

; Second pass with a more exact parser (and regex) - reads specific lines
user=> (def sols2 (atomic-list-from-file simple-file better-stock-message-parser))
#'user/sols2
user=> (count @sols2)
6

user=> (first @sols2)
{:price "2.30", :quantity "50", :stock "BAR", :action "Sell", :trade_time "05042013-13:24:13.123"}
```
#### single conditions and incrementers
```clojure
; in the context of a map composed of 'structure' keys
(defn name-action?
  "A predicate to check 'stock' name and 'action' against the current solution."
  [stock action]
  #(and (= stock (-> % :stock)) (= action (-> % :action))) )
    
; A count of all "Buy" actions of stock "FOO"
user=> (count-with-conditions @solutions (name-action? "FOO" "Buy"))
2    

; A running total of the :stock_price when 'predicates are true    
(def increment-with-stock-quanity
  "A running total for stock 'quantity' and 'count'."
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )
    
; Incrementing count based on stock quantity for each structure
user=> (count-with-conditions @sols2 (name? "FOO") increment-with-stock-quanity 0)
1200

; Collecting a sequence of all matching solutions
user=> (count (collect-with-conditions @sols2 (name-action? "FOO" "Buy")))
2
```
#### multiple conditions (all?) and (any?) to filter data
```clojure
(defn price-higher?
  "If a stock price is higher than X."
  [min]
  #(< min (read-string (-> % :price)) ) )

(defn price-lower?
  "If a stock price is lower than X."
  [max]
  #(> max (read-string (-> % :price)) ) )

; Using (all?)
user=> (count-with-conditions @sols2 (all? (name? "FOO") (price-higher? 12.1) (price-lower? 12.7) ) )
1

; Using (all?) and (any?) together
user=> (count-with-conditions @sols2 (all? (name? "FOO") (any? (price-higher? 12.20) (price-lower? 12.20)) ) )
2
```
### creating maps from key sets and regex groups
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
### example 4
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
### general notes
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
1. Retrieve code for stand alone use or as a resource
    * git clone
    * git submodule add  <your-project>/checkouts
2. Install Leiningen and update the **project.clj**
    * Adjust based on number and complexity of structured objects

```clojure
;; Increase to 1024m or 2096m for larger files
:jvm-opts ["-Xms256m" "-Xmx512m"]
```
```clojure
:repl-options { :init (do
                (load-file "checkouts/clash/src/clash/tools.clj")
                (load-file "checkouts/clash/src/clash/text_tools.clj")
                (load-file "checkouts/clash/src/clash/interact.clj")
                (load-file "your-clojure-source-file.clj")
                (use 'clash.tools)
                (use 'clash.text_tools)
                (use 'clash.interact)
                (use 'ns.your-clojure-file)
                (defn load-local-resource
                  [filename]
                  (str (System/getProperty "user.dir") "path/to/log-file" filename))
               )}
```


### notes
* requires "/bin/sh" functionality
* built with leiningen2 (thanks technomancy)
* clojure 1.5.1 (thank rich, et al)
* requires custom heap values and init in project.clj (sample forthcoming)
* first clojure foray

## License
Copyright (c) David Millett 2012. All rights reserved.
The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

