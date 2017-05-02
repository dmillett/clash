# Change Log

## 1.4.0
* Added `(pre-process)` to pre-process input from larger data sets where only a subset
of data is actually needed for analysis. Extract subsets of JSON with regex and before parsing.

## 1.3.3
* A collection optimization for `(haystack)` and `(pivot-matrix*)` with a significant
performance increase depending on distribution of data/query diversity.