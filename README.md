# clash
A clojure project for quick interactive analysis of structured text files (ex: logs, csv, etc.), within
the REPL. After parsing structured text into an in memory data structure as an *atom*, clash facilitates
counting and collecting results from the data structure based on specified predicates and incrementing
functions.

Clash also includes clojure-shell (bash, csh, zsh, ec) functionality that incorporates most of the speed 
advantages of commands like 'grep' and 'cut' individually or piped together. The results of unpiped/piped
shell command are OS specific (non MS Windows), but offer better performance than pure clojure/java 
grep/cut implementations.

## Features & Benefits
1. Quickly load small-large log or text file into an object structure 
    * See clash.interact/atomic-list-from-file
    * See clash.interact/atomic-map-from-file
    * See clash.text_tools/regex-group-into-map
    * Tested with 100 MB file with over 400,000+ complex object structures
2. Very fast, condition based, result counts and retrievals 
    * See clash.interact/count_with_conditions
    * See clash..interact/collect_with_conditions
    * Millisecond and subsecond results for nested queries on 400,000+ objects 
3. Quickly build and analyze data withing Clojure REPL
    * Existing log/text file data for patterns and ML
    * Experiment and identify optimal data queries for larger scale Hadoop style analysis
    * Determine initial trends
    
## Usage
Below is a brief summary of a simple example (see *test/clash/interact_test.clj*) included with 
this repository.

##### Start the clojure REPL

```
lein repl
```
##### Define object structure, regex, and parser for target file

```clojure
;; A log line example from (simple-structured.log)
(def line "05042013-13:24:12.000|sample-server|1.0.0|info|Buy,FOO,500,12.00")
 
; Defrecords offer a 12%-15% performance improvement during parsing
(def simple-stock-structure [:trade_time :action :stock :quantity :price])
(def detailed-stock-pattern #"(\d{8}-\d{2}:\d{2}:\d{2}.\d{3})\|.*\|(\w*),(\w*),(\d*),(.*)")

; Parse log line into 'simple-stock-structure' via 'detailed-stock-pattern'
(defn better-stock-message-parser
  "An exact parsing of line text into 'simple-stock-structure' using 'detailed-stock-pattern'."
  [line]
  (tt/regex-group-into-map line simple-stock-structure detailed-stock-pattern) )
```
##### Read file data structures into memory as a *list* or *map*

```clojure
; Create a reference atom of solutions to use in REPL 
(def solutions (atomic-list-from-file simple-file better-stock-message-parser))
```

##### test examples (see test/clash/interact_test.clj)
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

```clojure
; Ensure the lines were parsed and mapped properly
(= 6 (count @solutions)
    
(defn stock-name-action?
  "A predicate to check 'stock' name and 'action' against the current solution."
  [stock action]
  #(and (= stock (-> % :stock)) (= action (-> % :action))) )
    
; A count of all "Buy" actions of stock "FOO"
(= 2 (count-with-conditions @solutions (stock-name-action? "FOO" "Buy")))
    
(def increment-with-stock-quanity
  "Destructures 'solution' and existing 'count', and adds the stock 'quantity' and 'count'."
  (fn [solution count] (+ count (read-string (-> solution :quantity))) ) )
    
; Incrementing count based on stock quantity for each structure
(= 1200 (count-with-conditions @solutions (stock-name? "FOO") increment-with-stock-quanity))

; Collecting a sequence of all matching solutions
(= 2 (count (collect-with-condition @solutions (stock-name-action? "FOO" "Buy"))))
```
### example 2
```clojure
;; mapping regex groups: text, structure, pattern, sub-key(s) into a list of maps
    
; Return all keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)")
=> ({:a "a" :b "b"} {:a "c" :b "d"})    
    
; Only return ':a' keys
(regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)" [:a])
=> ({:a "a"} {:a "c"})
```
### example 3
```clojure
(def input1 (str tresource "/input1.txt"))
(def output1 (str tresource "/output1.txt"))
(def command1 (str "grep message " input1))

; Writes result to output1 (see test/command.clj)
(jproc-write command1 output1 "\n")
```
### example 4
```clojure
(def command2 (str "grep message " input1 " | cut -d\",\" -f2 " input1))
(def output2 (str tresource "/output2.txt"))

; Writes result to output2 (see test/command.clj)
(jproc-write command2 output2 ":")
```
### performance
I've personally used this to process large log files with over 400,000 complex data structures where
it takes ~30 seconds to load into memory, but less than a second for most complex queries thereafter.
Clojure *defrecord* objects offer a 12-15% performance improvement when parsing a file, but are slightly
less flexible to use.

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

