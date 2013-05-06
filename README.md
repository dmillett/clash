# clash
A combination of Clojure and Shell functionality that incorporates
most of the speed advantages of commands like 'grep' and 'cut' 
individually or piped together. The results of unpiped/piped
shell command are OS specific (non MS Windows), but offer better 
performance than pure clojure/java grep/cut implementations.

## Usage

### example 1

    (def input1 (str tresource "/input1.txt"))
    (def output1 (str tresource "/output1.txt"))
    (def command1 (str "grep message " input1))

    ; Writes result to output1 (see test/command.clj)
    (jproc-write command1 output1 "\n")

### example 2
    (def command2 (str "grep message " input1 " | cut -d\",\" -f2 " input1))
    (def output2 (str tresource "/output2.txt"))

    ; Writes result to output2 (see test/command.clj)
    (jproc-write command2 output2 ":")

### example 3
    ;; mapping regex groups: text, structure, pattern, sub-key(s) into a list of maps
    
    ; Return all keys
    (regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)")
    => ({:a "a" :b "b"} {:a "c" :b "d"})    
    
    ; Only return ':a' keys
    (regex-groups-into-maps "a,b,c,d" [:a :b] #"(\w),(\w)" [:a])
    => ({:a "a"} {:a "c"})

### performance
A simple performance test comparing '(re-split (re-find))' vs
'(jproc-write "grep | cut")' and a 145 MB file resulted in
completed in less than half the time with (jproc-write).

A performance macro that will adjust the time unit for
better readability and context. It will print out elapsed time
in nano seconds (ns), milliseconds (ms) or seconds(s). 

    (perf expr message)

    (def message2 "'cl + grep + cut'")
    (perf (jproc-write command2 output2 ":") message) --> 'cl + grep + cut' Time(ms):18.450

## notes
* requires "/bin/sh" functionality
* built with leiningen2 (thanks technomancy)
* clojure 1.5
* first clojure foray

## License
Copyright (c) David Millett 2012. All rights reserved.
The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

