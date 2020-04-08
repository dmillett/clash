(ns clash.example.covid19_worldmeter_test
  (:require [clojure.test :refer :all])
  (:use clash.example.covid19_worldmeter))

(def text0 "Nebraska \t412 \t+49 \t8 \t\t404 \t216 \t4 \t6,883 \t3,614 \t[1] [2] [3")
(def text1 "Wyoming \t212 \t+12 \t\t\t160 \t364 \t\t3,929 \t6,753 \t[1] [2]")
(def text2 "New York \t131,916 \t+8,898 \t4,758 \t+599 \t113,792 \t6,724 \t243 \t320,811 \t16,353 \t[1] [2] [3] [4] [5] [6] [7] [8]")

(deftest test-wm-cleanup
  (are [x y] (= x y)
   "Nebraska ,412 ,49 ,8 ,,404 ,216 ,4 ,6883 ,3614 ,[1] [2] [3" (wm-cleanup text0)
   "Wyoming ,212 ,12 ,,,160 ,364 ,,3929 ,6753 ,[1] [2]" (wm-cleanup text1)
    "New York ,131916 ,8898 ,4758 ,599 ,113792 ,6724 ,243 ,320811 ,16353 ,[1] [2] [3] [4] [5] [6] [7] [8]" (wm-cleanup text2)
    ))
