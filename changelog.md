# Change Log

## Next
 * (spec-from) Attempt to create spec structure for sets of data

## 1.5.2
### features
 * Added CSV parsing for (transform-lines) that creates keys from
   first CSV row for map or defrecord structures on the remaining rows
 * Added describe? to (shape) to indicate data structure types for keys  
   Ex: {"a": {"b": {"c": 42}}} -> a{}.b{}.c [42]
   
### bugs
 * Fixed JSON key-flatten inconsistency in some nested structures

## 1.5.1
### features
 * Analyze the value types with regular expressions for each flattened path
   with counts. Ex: "a.b.c" -> {"ints" 34 "doubles" 21 "strings" 11}

## 1.5.0
### features
 * Flatten XML and/or JSON structures into dot delimited node names.
   Ex: {"a": {"b": {"c": 42}}} -> a.b.c [42]

## 1.4.1
* Changed name of `(pre-process)` to `(disect)` as a more appropriate name (not bw compatible, but within 24 hrs)
* more docs

## 1.4.0
* Added `(pre-process)` to pre-process input from larger data sets where only a subset
of data is actually needed for analysis. Extract subsets of JSON with regex and before parsing.

## 1.3.3
* A collection optimization for `(haystack)` and `(pivot-matrix*)` with a significant
performance increase depending on distribution of data/query diversity.