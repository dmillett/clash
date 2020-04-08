(ns clash.example.covid19_worldmeter
  (:require [clash.core :as cc]
            [clojure.string :as s]))

(defn +values [values] (apply + (filter identity values)))
(defn *values [values] (apply * (filter identity values)))

(defn wm-cleanup
  "Copy and pasting data from a web form table seems to have a mix of whitespace
  and other formatting built in. This converts it to an actual CSV file."
  [line]
  (try
    (-> line
      (s/replace #"\t" "|")
      (s/replace #"," "")
      (s/replace #"\+" "")
      (s/replace #"\|" ",")
      (s/replace #",\s" ",,"))
    (catch Exception _ ("Skipping line"))))

;; State Total New Deaths Deaths_New Active Cases_per_million Deaths_per_million Total_Tests Tests_per_million Source
(defrecord CovidData [state total_pos new_cases deaths new_deaths active cases_million deaths_million
                      test_count tests_million sources])

(defn wm-parser
  "Cleans input data from its 'copy&paste from a UI table' to csv data. Any cleaning failures
  are printed to the screen in their raw and 'cleaned' state. "
  [text]
  (let [cleaned (wm-cleanup text)
        parts (s/split cleaned #",")
        state (s/trim (first parts))
        sources (last parts)
        data (map #(if (not-empty %) (read-string %) nil) (drop-last (rest parts)))]
    (try
      (apply ->CovidData (concat [state] data [sources]))
      (catch Exception _ (println "Failed:" text ",cleaned:" cleaned)))
    ))

(defn wm-percent-positive
  "A percentage for how many tests are positive?"
  [data]
  (try
    (when data
      {(:state data) {:total (:test_count data) :percent_positive (* 100.0 (/ (:total_pos data) (:test_count data)))}})
    (catch Exception _)
    ))

(defn wm-percent-uncertainty
  "What patients have symptoms but test negative? What is causing their symptoms?
  Are there test limitations (too early, too late) or quality issues?"
  [data]
  (try
    (when data
      (let [total (:test_count data)]
        {(:state data) {:total total :percent_unknown (* 100.0 (/ (- total (:total_pos data)) total))}}
        ) )
    (catch Exception _)
    ))

(defn wm-population
  "Find the population given the test count and event_per_million"
  [event_count events_per]
  (when (and event_count events_per)
    (int (/ event_count (/ events_per 1000000.0)))))

(defn wm-percentage
  "The percentage of the population for a specific event."
  [population event_count]
  (when (and population event_count)
    (* 100.0 (/ event_count population))))

(def maxkeys [:total_pos :deaths :cases_million :deaths_million :test_count :tests_million])

(defn compare-values
  [data mkeys results compfx]
  (reduce
    (fn [result k]
      (let [dv (get data k)
            rv (get result k)]
        (cond
          (and (nil? dv) (nil? rv)) result
          (nil? rv) (assoc result k dv)
          (nil? dv) (assoc result k rv)
          (compfx dv rv) (assoc result k dv)
          :default result)))
    results
    mkeys))

(defn wm-maximums
  "Find the maximum values in a set of data for a given set of keys."
  [data mkeys]
  (reduce-kv
    (fn [r _ v]
      (if (empty? r)
        (merge r (select-keys v maxkeys))
        (compare-values v mkeys r >)
        ))
    {}
    data))

(defn wm-calculate-percentages
  "More (and random) tests are better for mapping the spread, testing capacity and severity
   of new pathogens."
  [data maximums]
  (reduce-kv
    (fn [r _ v]
      (let [tpop (wm-population (:test_count v) (:tests_million v))
            testp (wm-percentage tpop (:test_count v))
            testpp (wm-percentage tpop (:total_pos v))
            testunk (wm-percentage tpop (- (:test_count v) (:total_pos v)))
            deathp (wm-percentage tpop (:deaths v))
            deathtestp (wm-percentage (:test_count v) (:deaths v))
            deathtestpp (wm-percentage (:total_pos v) (:deaths v))
            test_rmax (wm-percentage (:test_count maximums) (:test_count v))
            testp_rmax (wm-percentage (:total_pos maximums) (:total_pos v))
            testmillion_rmax (wm-percentage (:tests_million maximums) (:tests_million v))
            testpmillion_rmax (wm-percentage (:cases_million maximums) (:cases_million v))
            death_rmax (wm-percentage (:deaths maximums) (:deaths v))
            deathmillion_rmax (wm-percentage (:deaths_million maximums) (:deaths_million v))
            ]

        (assoc r (:state v) {:population tpop :tests (:test_count v) :positives (:total_pos v)
                             :deaths (:deaths v) :test_percent testp :test_positive_percent testpp
                             :test_unknown testunk :death_percent deathp :death_test_percent deathtestp
                             :death_test_positive_percent deathtestpp
                             :tests_relative_max test_rmax :tests_positive_relative_max testp_rmax
                             :tests_million_relative_max testmillion_rmax
                             :tests_positive_million_relative_max testpmillion_rmax :deaths_relative_max death_rmax
                             :deaths_million_relative_max deathmillion_rmax})
        ))
    {}
    data))
