# Example Usage: COVID-19 (SARS-CoV-2)

 * [Completed Tests](#completed-tests)
 * [Percent Positive Tests](#percent-positives)
 * [Percent Unknown Tests](#percent-uknown)
 
## Gather data 

For analyzing data, it is good to have a schema that has breadth and quantity. Finding Covid19 data that includes
the number of tests and positive results is helpful. Most sites just include the number of positive deaths and tests.
However, in new viral pandemics, this still leaves a lot of uncertainty about viral spread, test limitations and health
for the population.

There are more data resources from **Johns Hopkins** and **Tableau**, but finding CSVs that include more test data
than "active", "recovered", "fatal", location is required. I grabbed a sample data from the Miami Herald that I did not
include directly, but posted sample output.

 [Illinois](https://dph.illinois.gov/covid19)
 [Virginia](http://www.vdh.virginia.gov/coronavirus/)

### Worldmeter

Copy and paste table view into CSV
 [Worldmeter Coronavirus Data](https://www.worldometers.info/coronavirus/country/us/)

See **clash.example.covid19_worldmeter.clj**
 
```clojure
;; Original data as copy and pasted from worldmeter UI table
(def input "/media/dave/storage/dev/clash/test/resources/corona19-worldmeter-20200406.original")

;; Clean and import the data into a structure
(defrecord CovidData [state total_pos new_cases deaths new_deaths active cases_million deaths_million
                      test_count tests_million sources])

(def covid (transform-lines input wm-parser))

(first covid)
#clash.example.covid19_worldmeter.CovidData{:state "New York", :total_pos 131916, :new_cases 8898, :deaths 4758, 
:new_deaths 599, :active 113792, :cases_million 6724, :deaths_million 243, :test_count 320811, :tests_million 16353, 
:sources "[1] [2] [3] [4] [5] [6] [7] [8]"}
```

#### Calculate some percentages, relative ratios**

```clojure
;; Find maximum values for these keys
(def maxkeys [:total_pos :deaths :cases_million :deaths_million :test_count :tests_million])
(def maximums (wm-maximums covid maxkeys))
(def population-percentages (wm-calculate-percentages covid maximums))

;; Percentages across the entire population
(pprint (get population-percentages "New York"))
{:death_ratio_relative_max 1.0,
 :test_unknown_population_percent 0.9628722142487655,
 :test_positive_percent 41.11953767171325,
 :test_unknowns_percent 58.880462328286754,
 :positive 131916,
 :death_population_percent 0.02425340001268232,
 :test_positive_population_percent 0.6724278091788567,
 :death_test_positive_percent 3.606840716819794,
 :death_count 4758,
 :population 19617868,
 :test_count 320811,
 :test_ratio_relative_max 1.0,
 :test_population_percent 1.635300023427622,
 :test_positive_ratio_relative_max 1.0,
 :death_test_percent 1.483116227311408}

(pprint (get population-percentages "Florida"))
{:death_ratio_relative_max 0.053383775,
 :test_unknown_population_percent 0.5457380305992725,
 :test_positive_percent 10.81254760091394,
 :test_unknowns_percent 89.18745239908607,
 :positive 13629,
 :death_population_percent 0.001233042988927274,
 :test_positive_population_percent 0.06616197990586542,
 :death_test_positive_percent 1.8636730501137282,
 :death_count 254,
 :population 20599444,
 :test_count 126048,
 :test_ratio_relative_max 0.39290422,
 :test_population_percent 0.611900010505138,
 :test_positive_ratio_relative_max 0.10331575,
 :death_test_percent 0.2015105356689515}
```

#### Sort by relative Max

This starts to identify the 5 states with the highest positive test and death rates. One goal is a more even distribution of
test data across the US.

```clojure
(defn +values [values] (apply + (filter identity values)))
(defn *values [values] (apply * (filter identity values)))

;; Sort by relative maximums
(def relative_to_max [:test_ratio_relative_max :test_positive_ratio_relative_max :death_ratio_relative_max])
(def sorted_relative1 (sort-map-by-value population-percentages :ksubset relative_to_max :datafx +values))

(pprint (take 5 sorted_relative1))
(["New York"
  {:death_ratio_relative_max 1.0,
   :test_unknown_population_percent 0.9628722142487655,
   :test_positive_percent 41.11953767171325,
   :test_unknowns_percent 58.880462328286754,
   :positive 131916,
   :death_population_percent 0.02425340001268232,
   :test_positive_population_percent 0.6724278091788567,
   :death_test_positive_percent 3.606840716819794,
   :death_count 4758,
   :population 19617868,
   :test_count 320811,
   :test_ratio_relative_max 1.0,
   :test_population_percent 1.635300023427622,
   :test_positive_ratio_relative_max 1.0,
   :death_test_percent 1.483116227311408}]
 ["New Jersey"
  {:death_ratio_relative_max 0.21080285,
   :test_unknown_population_percent 0.5397729287809804,
   :test_positive_percent 46.151945367957595,
   :test_unknowns_percent 53.848054632042405,
   :positive 41090,
   :death_population_percent 0.01129265044360526,
   :test_positive_population_percent 0.46262712535168504,
   :death_test_positive_percent 2.4409832075930877,
   :death_count 1003,
   :population 8881883,
   :test_count 89032,
   :test_ratio_relative_max 0.27752167,
   :test_population_percent 1.002400054132665,
   :test_positive_ratio_relative_max 0.3114861,
   :death_test_percent 1.126561236409381}]
 ["Louisiana"
  {:death_ratio_relative_max 0.10760824,
   :test_unknown_population_percent 1.164312624769224,
   :test_positive_percent 21.49466500881936,
   :test_unknowns_percent 78.50533499118065,
   :positive 14867,
   :death_population_percent 0.010978619567245121,
   :test_positive_population_percent 0.3187873771606116,
   :death_test_positive_percent 3.443868971547723,
   :death_count 512,
   :population 4663610,
   :test_count 69166,
   :test_ratio_relative_max 0.21559735,
   :test_population_percent 1.483100001929835,
   :test_positive_ratio_relative_max 0.11270051,
   :death_test_percent 0.7402480987768557}]
 ["Washington"
  {:death_ratio_relative_max 0.08007566,
   :test_unknown_population_percent 1.13855527728439,
   :test_positive_percent 9.111901504787962,
   :test_unknowns_percent 90.88809849521205,
   :positive 8326,
   :death_population_percent 0.005223296615797333,
   :test_positive_population_percent 0.1141447969110987,
   :death_test_positive_percent 4.576026903675234,
   :death_count 381,
   :population 7294244,
   :test_count 91375,
   :test_ratio_relative_max 0.28482503,
   :test_population_percent 1.252700074195489,
   :test_positive_ratio_relative_max 0.063115925,
   :death_test_percent 0.4169630642954856}]
 ["Massachusetts"
  {:death_ratio_relative_max 0.05464481,
   :test_unknown_population_percent 0.9164119613781083,
   :test_positive_percent 18.10438446139554,
   :test_unknowns_percent 81.89561553860446,
   :positive 13837,
   :death_population_percent 0.00380667034059158,
   :test_positive_population_percent 0.2025880673183296,
   :death_test_positive_percent 1.8790200187902,
   :death_count 260,
   :population 6830116,
   :test_count 76429,
   :test_ratio_relative_max 0.23823684,
   :test_population_percent 1.1190000286964379,
   :test_positive_ratio_relative_max 0.10489251,
   :death_test_percent 0.3401850083083646}])
```

#### Sort by Positive/Unknown tests and Death percents

A rough idea of testing effectiveness and how it relates to deaths.

```clojure
;; Sort by test/death percentages
(def severity1 [:test_population_percent :test_unknowns_percent :test_positive_percent :death_percent :death_test_percent])
(def sorted1 (sort-map-by-value population-percentages :ksubset sort1 :datafx +values))

(pprint (take 5 test_death_percents))
(["Oklahoma"
  {:death_ratio_relative_max 0.010718789,
   :test_unknown_population_percent 0.03633762656607132,
   :test_positive_percent 48.237004725554335,
   :test_unknowns_percent 51.76299527444566,
   :positive 1327,
   :death_population_percent 0.001301417805385981,
   :test_positive_population_percent 0.03386238093621955,
   :death_test_positive_percent 3.8432554634513942,
   :death_count 51,
   :population 3918803,
   :test_count 2751,
   :test_ratio_relative_max 0.008575142,
   :test_population_percent 0.07020000750229088,
   :test_positive_ratio_relative_max 0.010059432,
   :death_test_percent 1.853871319520174}]
 ["New Jersey"
  {:death_ratio_relative_max 0.21080285,
   :test_unknown_population_percent 0.5397729287809804,
   :test_positive_percent 46.151945367957595,
   :test_unknowns_percent 53.848054632042405,
   :positive 41090,
   :death_population_percent 0.01129265044360526,
   :test_positive_population_percent 0.46262712535168504,
   :death_test_positive_percent 2.4409832075930877,
   :death_count 1003,
   :population 8881883,
   :test_count 89032,
   :test_ratio_relative_max 0.27752167,
   :test_population_percent 1.002400054132665,
   :test_positive_ratio_relative_max 0.3114861,
   :death_test_percent 1.126561236409381}]
 ["New York"
  {:death_ratio_relative_max 1.0,
   :test_unknown_population_percent 0.9628722142487655,
   :test_positive_percent 41.11953767171325,
   :test_unknowns_percent 58.880462328286754,
   :positive 131916,
   :death_population_percent 0.02425340001268232,
   :test_positive_population_percent 0.6724278091788567,
   :death_test_positive_percent 3.606840716819794,
   :death_count 4758,
   :population 19617868,
   :test_count 320811,
   :test_ratio_relative_max 1.0,
   :test_population_percent 1.635300023427622,
   :test_positive_ratio_relative_max 1.0,
   :death_test_percent 1.483116227311408}]
 ["Michigan"
  {:death_ratio_relative_max 0.15279528,
   :test_unknown_population_percent 0.2864672612717592,
   :test_positive_percent 37.64317565795226,
   :test_unknowns_percent 62.356824342047744,
   :positive 17221,
   :death_population_percent 0.007300511758844916,
   :test_positive_population_percent 0.1729327551569028,
   :death_test_positive_percent 4.221589919284594,
   :death_count 727,
   :population 9958206,
   :test_count 45748,
   :test_ratio_relative_max 0.1426011,
   :test_population_percent 0.4594000164286619,
   :test_positive_ratio_relative_max 0.1305452,
   :death_test_percent 1.5891405088747048}]
 ["Northern Mariana Islands"
  {:death_ratio_relative_max 2.1017234E-4,
   :test_unknown_population_percent nil,
   :test_positive_percent 24.24242424242424,
   :test_unknowns_percent 75.75757575757575,
   :positive 8,
   :death_population_percent nil,
   :test_positive_population_percent nil,
   :death_test_positive_percent 12.5,
   :death_count 1,
   :population nil,
   :test_count 33,
   :test_ratio_relative_max 1.028643E-4,
   :test_population_percent nil,
   :test_positive_ratio_relative_max 6.0644652E-5,
   :death_test_percent 3.03030303030303}])
```


### Miami Herald (courtesy of a download link on their site last week)

I cannot find the original link, but this file is from Florida Department of Health:
https://www.miamiherald.com/news/coronavirus/article241410296.html?intcid=pushly_498044


```clojure
;; From Miami Herald CSV data (thanks to them)
(defrecord MiamiHeraldCovid19Data [state_code state positive death total negative pending updated x pp])

(defn covid19-miamiherald-csv-parser
  "A CSV parser that creates a defrecord from a 'defrecord and a line of data. "
  [line]
  (try
    (when (not-empty line)
      (apply ->MiamiHeraldCovid19Data (s/split line #",")))
    (catch Exception e (println e))
    ) )

;; Loads in a millisecond or two
(def covid19 (transform-lines "/path/to/miamiherald/data-aJFtO.csv" covid19-miamiherald-csv-parser))
```

<a name="completed-tests"/></a>
### Completed Tests

In a pandemic, such as Covid19, it is important to gather as much data as possible about infection rates. 
Symptoms can be useful indicators, but proper medical testing is the best approach (PCR, antibody, culture, etc).
The more tests applied to the population, regardless of symptoms, the easier it is to map the pathogen spread.

If states are not testing at high levels, there is a lot more uncertainty about the spread of a virus. Valuable
questions include:

If a region is not testing its population, why not?
  - Is there a lack of tests?
  - Are people not symptomatic and not getting tested?
  - Is the local government discouraging/not testing its population?

Questions from data:
 1. Why are there so few tests in Oklahoma, Montana, Rhode Island, etc?
   - What role does population density play? - more data needed
   - How many tests are available?
   - How do people get tested?
 2. Why are New York and Florida able to test more than California and Texas?
   - New York has far more tested infections than anywhere else in the US

```clojure
(defn tests-issued
  "Grab the total number of tests issued per state."
  [data]
  (try
    (when data
      {(:state data) (Integer/parseInt (:total data))})
     (catch Exception _)
     ) )

;; Determine how many tests were issued and merge into a single map
(def covid19tests (apply merge (map #(tests-issued %) covid19)))
(def covid19tests_sorted (sort-map-by-value covid19tests))

(pprint covid19tests_sorted)
{"New York" 271002,
 "Florida" 95800,
 "California" 94800,
 "Washington" 82599,
 "New Jersey" 67503,
 "Massachusetts" 62962,
 "Pennsylvania" 62115,
 "Texas" 55764,
 "Louisiana" 53645,
 "Illinois" 48048,
 "Ohio" 38375,
 "Tennessee" 37839,
 "North Carolina" 31598,
 "Missouri" 26840,
 "Georgia" 25401,
 "Arizona" 24673,
 "Michigan" 24637,
 "Wisconsin" 24289,
 "Utah" 24248,
 "Minnesota" 24227,
 "Maryland" 23690,
 "Colorado" 22071,
 "Connecticut" 20016,
 "Virginia" 19005,
 "Indiana" 17835,
 "Oregon" 17434,
 "South Carolina" 16159,
 "New Mexico" 15632,
 "Kentucky" 15572,
 "Nevada" 14532,
 "Hawaii" 12278,
 "Arkansas" 9791,
 "Alabama" 9722,
 "Iowa" 9453,
 "Idaho" 8870,
 "New Hampshire" 7599,
 "Kansas" 7074,
 "Maine" 6520,
 "West Virginia" 6367,
 "Mississippi" 6111,
 "Montana" 6057,
 "Alaska" 6016,
 "North Dakota" 5798,
 "District of Columbia" 5584,
 "Delaware" 5445,
 "Vermont" 5228,
 "Rhode Island" 5123,
 "Nebraska" 4875,
 "South Dakota" 4783,
 "Wyoming" 2940,
 "Oklahoma" 2303}
```

<a name="percent-positive"/></a>
#### States with most (+) cases per tests issued
These are the cases where people presumably had symptoms and were able to get tested AND the tests were positive.

Questions:
 1. Over 50% of Michigan's tests are positive
   - Why is the rate so high in Michigan?
   - What are the restrictions around getting tested?
   - How similar are the symptoms that Michigan requires for testing?
   - What is similar in the areas where they are testing?  
 2. Oklahoma also has a high positive test percentage
   - Where is Oklahoma testing that its got a high positive rate?
   - Why hasn't Oklahoma issued more tests?

```clojure
(defn test-percent-positive
  "A percentage for how many tests are positive?"
  [data]
  (try
    (when data
      (let [total (read-string (:total data))]
        {(:state data) {:total total :percent_positive (* 100.0 (/ (read-string (:positive data)) total))}}
        ) )
    (catch Exception _)
    ))

;; US states with highest (+) test percentage (resource: MiamiHeral data 20200404)
(def covid19tpp (apply merge (map #(test-percent-positive %) covid19)))
(def covid19tpp_sorted (sort-map-by-value covid19tpp :ksubset [:percent_positive]))

(def covid19tpp_sorted (sort-map-by-value covid19tpp :ksubset [:percent_positive]))
(pprint covid19tpp_sorted)
{"Michigan" {:total 24637, :percent_positive 51.72707716036855},
 "New Jersey" {:total 67503, :percent_positive 44.2869205813075},
 "Oklahoma" {:total 2303, :percent_positive 42.900564481111594},
 "New York" {:total 271002, :percent_positive 37.95654644615169},
 "Connecticut" {:total 20016, :percent_positive 24.55535571542766},
 "Georgia" {:total 25401, :percent_positive 23.49120113381363},
 "Mississippi" {:total 6111, :percent_positive 22.22222222222222},
 "Indiana" {:total 17835, :percent_positive 19.271096159237448},
 "Louisiana" {:total 53645, :percent_positive 19.19470593717961},
 "Colorado" {:total 22071, :percent_positive 18.90716324588827},
 "Illinois" {:total 48048, :percent_positive 18.53146853146853},
 "Massachusetts" {:total 62962, :percent_positive 16.5210762046949},
 "Alabama" {:total 9722, :percent_positive 15.78893231845299},
 "Rhode Island" {:total 5123, :percent_positive 13.87858676556705},
 "District of Columbia" {:total 5584, :percent_positive 13.55659025787966},
 "Pennsylvania" {:total 62115, :percent_positive 13.55550189165258},
 "Maryland" {:total 23690, :percent_positive 11.64204305614183},
 "Idaho" {:total 8870, :percent_positive 11.42051860202931},
 "California" {:total 94800, :percent_positive 11.287974683544299},
 "Florida" {:total 95800, :percent_positive 10.71816283924843},
 "Virginia" {:total 19005, :percent_positive 10.58668771375954},
 "South Carolina" {:total 16159, :percent_positive 10.5204529983291},
 "Nevada" {:total 14532, :percent_positive 10.41838700798238},
 "Texas" {:total 55764, :percent_positive 9.558137866724051},
 "Kansas" {:total 7074, :percent_positive 8.764489680520215},
 "Ohio" {:total 38375, :percent_positive 8.630618892508144},
 "Washington" {:total 82599, :percent_positive 8.433516144263248},
 "Delaware" {:total 5445, :percent_positive 8.264462809917356},
 "Tennessee" {:total 37839, :percent_positive 8.105393905758609},
 "Missouri" {:total 26840, :percent_positive 7.8725782414307},
 "Wisconsin" {:total 24289, :percent_positive 7.871876157931575},
 "Arkansas" {:total 9791, :percent_positive 7.537534470432029},
 "Vermont" {:total 5228, :percent_positive 7.4407039020658},
 "Iowa" {:total 9453, :percent_positive 7.394477943509997},
 "Arizona" {:total 24673, :percent_positive 7.169780731974224},
 "New Hampshire" {:total 7599, :percent_positive 7.106198183971575},
 "Maine" {:total 6520, :percent_positive 6.625766871165643},
 "North Carolina" {:total 31598, :percent_positive 6.623836951705804},
 "Nebraska" {:total 4875, :percent_positive 5.846153846153846},
 "Wyoming" {:total 2940, :percent_positive 5.646258503401361},
 "Kentucky" {:total 15572, :percent_positive 5.336501412792191},
 "Oregon" {:total 17434, :percent_positive 5.156590570150281},
 "Utah" {:total 24248, :percent_positive 5.138568129330253},
 "Montana" {:total 6057, :percent_positive 4.325573716361235},
 "South Dakota" {:total 4783, :percent_positive 3.9096801170813302},
 "West Virginia" {:total 6367, :percent_positive 3.722318203235433},
 "Minnesota" {:total 24227, :percent_positive 3.2566970735130227},
 "New Mexico" {:total 15632, :percent_positive 3.166581371545548},
 "North Dakota" {:total 5798, :percent_positive 2.983787512935495},
 "Alaska" {:total 6016, :percent_positive 2.609707446808511},
 "Hawaii" {:total 12278, :percent_positive 2.598143020035836}}
```

<a name="percent-uknown"/></a>
#### Percent Uknown Tests

In these cases, people presumably had symptoms and had a negative Covid 19 test result. That should lead to
additional questions and tests. For example:

 * What are the test limitations?
   - a defective swab or no sign of infection in swabbed area?
   - inability to identify virus or virions due to poor material?
   - improper lab process or equipment?

 * What else was the patient tested for that might produce similar symptoms?
   - Influenza A/B/C
   - other?
   - anxiety/anxiety+chronic?

```clojure
(defn test-percent-uncertainty
  "What patients have symptoms but test negative? What is causing their symptoms?
  Are there test limitations (too early, too late) or quality issues?"
  [data]
  (try
    (when data
      (let [total (read-string (:total data))]
        {(:state data) {:total total :percent_unknown (* 100.0 (/ (- total (read-string (:positive data))) total))}}
        ) )
    (catch Exception _)
    ))

(def covid19tpunk (apply merge (map #(test-percent-uncertainty %) covid19)))
(def covid19tpunk_sorted (sort-map-by-value covid19tpunk :ksubset [:percent_unknown]))

(pprint covid19tpunk_sorted)
{"Hawaii" {:total 12278, :percent_unknown 97.40185697996417},
 "Alaska" {:total 6016, :percent_unknown 97.3902925531915},
 "North Dakota" {:total 5798, :percent_unknown 97.01621248706451},
 "New Mexico" {:total 15632, :percent_unknown 96.83341862845445},
 "Minnesota" {:total 24227, :percent_unknown 96.74330292648699},
 "West Virginia" {:total 6367, :percent_unknown 96.27768179676457},
 "South Dakota" {:total 4783, :percent_unknown 96.09031988291868},
 "Montana" {:total 6057, :percent_unknown 95.67442628363877},
 "Utah" {:total 24248, :percent_unknown 94.86143187066975},
 "Oregon" {:total 17434, :percent_unknown 94.84340942984973},
 "Kentucky" {:total 15572, :percent_unknown 94.66349858720781},
 "Wyoming" {:total 2940, :percent_unknown 94.35374149659864},
 "Nebraska" {:total 4875, :percent_unknown 94.15384615384616},
 "North Carolina" {:total 31598, :percent_unknown 93.3761630482942},
 "Maine" {:total 6520, :percent_unknown 93.37423312883436},
 "New Hampshire" {:total 7599, :percent_unknown 92.89380181602841},
 "Arizona" {:total 24673, :percent_unknown 92.83021926802579},
 "Iowa" {:total 9453, :percent_unknown 92.60552205649},
 "Vermont" {:total 5228, :percent_unknown 92.5592960979342},
 "Arkansas" {:total 9791, :percent_unknown 92.46246552956798},
 "Wisconsin" {:total 24289, :percent_unknown 92.12812384206843},
 "Missouri" {:total 26840, :percent_unknown 92.1274217585693},
 "Tennessee" {:total 37839, :percent_unknown 91.8946060942414},
 "Delaware" {:total 5445, :percent_unknown 91.73553719008264},
 "Washington" {:total 82599, :percent_unknown 91.56648385573675},
 "Ohio" {:total 38375, :percent_unknown 91.36938110749186},
 "Kansas" {:total 7074, :percent_unknown 91.23551031947979},
 "Texas" {:total 55764, :percent_unknown 90.44186213327595},
 "Nevada" {:total 14532, :percent_unknown 89.58161299201763},
 "South Carolina" {:total 16159, :percent_unknown 89.4795470016709},
 "Virginia" {:total 19005, :percent_unknown 89.41331228624047},
 "Florida" {:total 95800, :percent_unknown 89.28183716075156},
 "California" {:total 94800, :percent_unknown 88.7120253164557},
 "Idaho" {:total 8870, :percent_unknown 88.5794813979707},
 "Maryland" {:total 23690, :percent_unknown 88.35795694385817},
 "Pennsylvania" {:total 62115, :percent_unknown 86.44449810834742}, 
"District of Columbia" {:total 5584, :percent_unknown 86.44340974212034},
 "Rhode Island" {:total 5123, :percent_unknown 86.12141323443295},
 "Alabama" {:total 9722, :percent_unknown 84.21106768154701},
 "Massachusetts" {:total 62962, :percent_unknown 83.4789237953051},
 "Illinois" {:total 48048, :percent_unknown 81.46853146853147},
 "Colorado" {:total 22071, :percent_unknown 81.09283675411173},
 "Louisiana" {:total 53645, :percent_unknown 80.8052940628204},
 "Indiana" {:total 17835, :percent_unknown 80.72890384076254},
 "Mississippi" {:total 6111, :percent_unknown 77.77777777777779},
 "Georgia" {:total 25401, :percent_unknown 76.50879886618637},
 "Connecticut" {:total 20016, :percent_unknown 75.44464428457233},
 "New York" {:total 271002, :percent_unknown 62.0434535538483},
 "Oklahoma" {:total 2303, :percent_unknown 57.099435518888406},
 "New Jersey" {:total 67503, :percent_unknown 55.7130794186925},
 "Michigan" {:total 24637, :percent_unknown 48.27292283963145}
}
```