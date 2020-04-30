(ns clash.example.covid19_worldmeter
  (:require
    [clash.core :as cc]
    [clojure.string :as s]
    [clojure.spec.alpha :as cs]
    [clash.tools :as ct]
    ;[incanter.core :as ic]
    ;[incanter.stats :as is]
    ))

(defn todo
  [msg & args]
  (println "todo:" msg ", args:" args))

(defn sort-values
  "Apply a function for similar data for a list of values or list of arrays. For example:
  (sort-values [2 3] +)
  5

  (sort-values [[2 3] [4 5]] *)
  [8 15]
  "
  [values fx & {:keys [dvfx] :or {dvfx nil}}]
  (let [arrays (if dvfx (dvfx values) values)
        fvalue (first arrays)]
    (cond
      ((ct/none? #(coll? %) #(map? %)) fvalue) (apply fx arrays)
      (coll? fvalue) (reduce
                       (fn [r index] (conj r (apply fx (map #(nth % index) arrays))))
                        []
                        (range 0 (count fvalue)))
      :default nil)
    ) )

(defn +values
  "Add all the values specified in a collection. Designed for use
  with (sort-map-by-value :datafx)"
  [values]
  (apply + (filter identity values)))

(defn ++values
  [values]
  (let [arrays (map #(replace {nil 0} %) values)]
    (reduce
      (fn [r index] (conj r (apply + (map #(nth % index) arrays))))
      []
      (range 0 (count (first arrays))))
    ) )

(defn *values
  "Multiply all the values specified in a collection. Designed for use
  with (sort-map-by-value :datafx)"
  [values]
  (apply * (replace {nil 1} values)))

(defn **values
  "Multiply the index value in a collection of collections (array). Designed
  for use with (sort-map-by-value :datafx)  Where :ksubset [k1 k2 k3] results
  in [[k1x k1y] [k2x k2y] [k3x k3y]]"
  [values]
  (try
    (let [arrays (map #(replace {nil 1} %) values)]
      (reduce
        (fn [r index] (conj r (apply * (map #(nth % index) arrays))))
        []
        (range 0 (count (first arrays))))
      )
    (catch Exception _ (println values))) )

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

(defn wm-population
  "Find the population given the test count and event_per_million"
  [event_count events_per]
  (when (and event_count events_per)
    (int (/ event_count (/ events_per 1000000.0)))))

(defn wm-percentage
  "The percentage of the population for a specific event."
  [population event_count & {:keys [percent] :or {percent true}}]
  (when (and event_count population (not (zero? population)))
    (if percent
      (* 100.0 (/ event_count population))
      (* 1.0 (/ event_count population)))))

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

(defn- default-value
  [obj default]
  (if obj obj default))

(defn- tc-wrap
  [fx data default & {:keys [debug] :or {debug false}}]
  (if data
    (try (apply fx data) (catch Exception e (when debug (println "Args:" data ", Error:" e))))
    default))

(defn wm-percentages
  "More (and random) tests are better for mapping the spread, testing capacity and severity
   of new pathogens."
  [data maximums]
  (reduce-kv
    (fn [r _ v]
      (let [population (wm-population (:test_count v) (:tests_million v))
            testcount (wm-percentage population (:test_count v))
            testpopulationpositive (wm-percentage population (:total_pos v))
            testpositive (wm-percentage (:test_count v) (:total_pos v))
            testunknown (wm-percentage (:test_count v) (tc-wrap - [(:test_count v) (:total_pos v)] nil))
            testpopulationunknown (wm-percentage population (tc-wrap - [(:test_count v) (:total_pos v)] nil))
            deathpopulation (wm-percentage population (:deaths v))
            deathtestpopulation (wm-percentage (:test_count v) (:deaths v))
            deathpositivepopulation (wm-percentage (:total_pos v) (:deaths v))
            test_rmax (wm-percentage (:test_count maximums) (:test_count v) :percent false)
            testp_rmax (wm-percentage (:total_pos maximums) (:total_pos v) :percent false)
            death_rmax (wm-percentage (:deaths maximums) (:deaths v) :percent false)
            ]

        (assoc r (:state v) {:population                       population
                             :death_count                      (:deaths v)
                             :death_population_percent         deathpopulation
                             :death_test_percent               deathtestpopulation
                             :death_test_positive_percent      deathpositivepopulation
                             :death_ratio_relative_max         death_rmax
                             :test_count                       (:test_count v)
                             :test_positive_count              (:total_pos v)
                             :test_positive_percent            testpositive
                             :test_unknowns_percent            testunknown
                             :test_population_percent          testcount
                             :test_positive_population_percent testpopulationpositive
                             :test_unknown_population_percent  testpopulationunknown
                             :test_ratio_relative_max          test_rmax
                             :test_positive_ratio_relative_max testp_rmax
                             })
        ))
    {}
    data))

(defn merge-percentages
  "For merging daily percentages together and start to establish trends,
  each of the test/mortality data is conj'd into a collection."
  [& mps]
  (apply merge-with
    (fn [mleft mright]
      (merge-with
        #(if (coll? %1)
           (conj %1 %2)
           [%1 %2])
        mleft mright)
      )
    mps))


(def percentage-keys [:population :death_count :death_population_percent :death_test_percent :death_test_positive_percent
                      :death_ratio_relative_max :test_count :test_positive_count :test_positive_percent
                      :test_unknowns_percent :test_population_percent :test_positive_population_percent
                      :test_unknown_population_percent :test_ratio_relative_max :test_positive_ratio_relative_max])

;; Sort by relative maximums
(def relative_to_max [:test_ratio_relative_max :test_positive_ratio_relative_max :death_ratio_relative_max])
(def relative_pr_focus (conj relative_to_max :test_count :death_count))

(def death_test_percents [:test_population_percent :test_unknowns_percent :test_positive_percent :death_percent :death_test_percent])
(def death_percents [:death_test_positive_percent :death_test_percent :death_population_percent])
(def tests [:test_count :death_count :test_positive_percent])

;; todo: kind of ugly duplication here to cleanup
(defn pr-percentages
  "Print out sort, ratio percentages from (wm-percentages) output. This keeps the keys in a specific
  order when printing key:value data."
  [data & {:keys [focus] :or {focus nil}}]
  (let [prefx (fn [value] (if (and focus (some #{value} focus)) (str "**" value ":") (str " " value ":")))
        prmapfx (fn [v] (map #(str (prefx %) (% v) ",\n") percentage-keys))
        prvalfx (fn [v] (map #(str (prefx %) v ",\n") percentage-keys))]
    (if (map? (second (first data)))
      (println (for [[k v] data] (apply str (str "\n" k ":\n") (prmapfx v))))
      (println (for [[k v] data] (apply str (str "\n" k ":\n") (prvalfx v))))
      ) ) )

(defn input-files
  "Find the daily Worldmeter files."
  [^String dirpath]
  (filter #(not= dirpath (str %)) (file-seq (clojure.java.io/file dirpath))))

;; todo Probably a transducer would be better here...
(defn wm-daily-workflow
  "Provide this function with directory to pull Worldmeter data from and the collection
  of keys to determine maximum values for. The result is a map of percentage date, by date,
  for the underlying file.

  (wm-daily-workflow \"/path/to/files\" maxkeys)

  {\"us_20200407\" {\"New York\" {} \"New Jersey\" {}, ...}}
  "
  [dirpath maxkeys]
  (let [namefn (fn [fname] (let [[_ _ c dt] (-> fname (s/replace #".original" "") (s/split #"-"))] (str c "_" dt)))
        inputs (input-files dirpath)
        covids (reduce (fn [r input] (assoc r (namefn input) (cc/transform-lines input wm-parser))) {} inputs)
        maximums (reduce-kv (fn [r k v] (assoc r k (wm-maximums v maxkeys))) {} covids)
        ]
    (reduce-kv (fn [r k v] (assoc r k (wm-percentages v (get maximums k)))) {} covids)
    ))

;; todo: Make lazy
(defn deltas
  "Find the deltas and ~gradient of a list of numbers. Anything > 1.0 indicates
  an increasing slope, while < 1.0 indicates a decreasing slope. "
  [coll]
  (let [c (if (coll? coll) coll [coll])]
    (loop [x0 (first c)
           data (rest c)
           deltas []]
      (if (empty? data)
        deltas
        (let [x1 (first data)
              delta (when (and x0 x1) (- x1 (double x0)))]
          (recur x1 (rest data) (conj deltas delta)))))
      ) )

(defn gradient
  "Calculate the gradients for a collection of numbers. In this case

  deltaY(n-1) = Y(n-1) - Y(n-2)
  deltaY(n) = Y(n) - Y(n-1)

  (deltaY(n) - deltaY(n-1))/ deltaY(n-1)

  Since the stats are calucated 1/day, X = 1.
  "
  [coll & {:keys [units] :or {units 1}}]
  (for [n (range 1 (count coll))]
    (double (cond
      (zero? n) (nth coll n)
      (= 1 n) (- (nth coll 1) (first coll))
      :default (let [vpp (nth coll (- n 2))
                     vp (nth coll (dec n))
                     v (nth coll n)]
                 (/ (- v vp) (if (= vpp vp) 1 (Math/abs (- vp vpp))))))
      ) ) )

(defn- combined-deltas
  [combined_percentages & {:keys [ks n] :or {ks nil n nil}}]
  (reduce-kv
    (fn [r k v] (assoc r k (reduce-kv (fn [r1 k1 v1] (assoc r1 k1 (deltas (if n (take n v1) v1)))) {} v)))
    {}
    combined_percentages))

(defn mean
  "Calculate the average or mean from a sequence of numbers."
  [coll]
  (try
    (if (and (coll? coll) (not (empty? coll)))
      (double (/ (reduce + (filter identity coll)) (count coll)))
      coll
      )
    (catch Exception _)))

(defn combine-percentages
  "Combine all or the last 'ndays' of days from daily percentages until a single combined map."
  ([percentages] (combine-percentages percentages -1))
  ([percentages ndays]
   (let [combined (apply merge-percentages (vals (sort percentages)))]
     (if (= -1 ndays)
       combined
       (reduce-kv
         (fn [r k v] (assoc r k (reduce-kv (fn [r1 k1 v1] (assoc r1 k1 (take ndays v1)) ) {} v)))
         {}
         combined))
     ) ) )

(def report_keys
  {:avg "avg" :avg_5days_until "avg-5-days-until" :avg_10days_until "avg-10-days-until" :avg_20days_until "avg-20-days-until"
   :grad "grad" :grad_5days_until "grad-5-days-until" :grad_10days_until "grad-10-days-until" :grad_20days_until "grad-20-days-until"})


(defn avg-5-10-20-all
  [& {:keys [to from] :or {to nil from nil}}]
  [;{:fx #(mean (take 3 %)) :name "average-3-day-from" :date from}
   ;{:fx #(mean (take 5 %)) :name "average-5-day-from" :date from}
   ;{:fx #(mean (take 10 %)) :name "average-10-day-from" :date from}
   {:fx #(mean (take 5 (reverse %))) :name (:avg_5days_until report_keys) :date to}
   {:fx #(mean (take 10 (reverse %))) :name (:avg_10days_until report_keys) :date to}
   {:fx #(mean (take 20 (reverse %))) :name (:avg_20days_until report_keys) :date to}
   {:fx #(mean %) :name (:avg report_keys)}
   {:fx #(mean (drop (- (count %) 5) (gradient %))) :name (:grad_5days_until report_keys) :date to}
   {:fx #(mean (drop (- (count %) 10) (gradient %))) :name (:grad_10days_until report_keys) :date to}
   {:fx #(mean (drop (- (count %) 20) (gradient %))) :name (:grad_20days_until report_keys) :date to}
   {:fx #(mean (gradient %)) :name "grad"}
   ])

(defn combined-functions
  "Run data for combined value data through a function or functions. For example,
  determine the average/mean for data like ':death_test_percent'. Each 'fx' form
  must be passed in as {:fx (mean) :name avg} in a sequence. [fx1 fx2 fx3 etc]
  (combined-functions combined-data avg-3-5-10"
  [combined fxs]
  (reduce-kv
    (fn [r k v] (assoc r k
                         (reduce-kv
                           (fn [r1 k1 v1]
                             (let [done (if (coll? v1) v1 [v1])
                                   calcs (reduce
                                           (fn [r fx]
                                             (assoc r (:name fx) ((:fx fx) done)))
                                           {}
                                           fxs)]
                              (assoc r1 k1 (merge calcs {:data v1}))))
                           {}
                           v)))
    {}
    combined) )

(defn wm-daily-sorts
  "Given a map of percentage data, this will provide different sorting outcomes. It also provides
  a map of all days merged together, where their keys point to a collection of data (by day).
    {\"us_20200407\" {\"New York\" {:death_count [4 5 6]} \"New Jersey\" {:death_count [1 2 3]}, ...}}
  "
  [percentages]
  (let [combined (apply merge-percentages (vals (sort percentages)))
        date_sort (sort (keys percentages))
        from (first date_sort)
        to (last date_sort)]
    {:daily_tests_deaths (reduce-kv (fn [r k v] (assoc r k (ct/sort-map-by-value v :ksubset death_test_percents :datafx +values))) {} percentages)
     :daily_relatives (reduce-kv (fn [r k v] (assoc r k (ct/sort-map-by-value v :ksubset relative_to_max :datafx *values))) {} percentages)
     :dates (keys (sort percentages))
     :combined combined
     :combined_averages (combined-functions combined (avg-5-10-20-all :to to :from from))
     :combined_tests_deaths (ct/sort-map-by-value combined :ksubset death_test_percents :datafx ++values)
     :combined_relatives (ct/sort-map-by-value combined :ksubset relative_to_max :datafx **values)
     :combined_death (ct/sort-map-by-value combined :ksubset death_percents :datafx **values)
     }))


(def death_and_death_percents (conj [:deaths] death_percents))

(defn report-averages-gradients
  "Grab the top 10 (n) states, in descending order, averages and gradients for a specific metric.
  (report-averages-gradients (:combined_averages daily_sorts) death_percents (vals report_keys) :n 10 :dt )
  "
  [combined data_keys report_keys & {:keys [n dt] :or {n 10 dt ""}}]
  (reduce
    (fn [r k]
      (assoc r k
        (reduce
          (fn [r1 k1]
            (assoc r1 (keyword (str k1 dt)) (take n (keys (ct/sort-map-by-value combined :ksubpath [k] :ksubset [k1])))))
          {}
          report_keys))
      )
    {}
    data_keys)
  )

(defn print-report-avgs-grads
  "A more human readable format for avergae and gradient data from (report-averages-gradients)"
  [data]
  (print
    (for [[s k] data]
      (apply str "\n"
        (for [[m d] k]
          (str s "_" m "=" (apply str (interpose "," d)) "\n")
          )))
    ) )