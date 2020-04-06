# Example Usage: COVID-19 (SARS-CoV-2)

 * [Percent Positive Tests](#percent-positives)
 * [Percent Unknown Tests](#percent-uknown)
 
## Gather data 

### Miami Herald

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

<a name="percent-positive"/></a>
#### States with most (+) cases per tests issued
These are the cases where people presumably had symptoms and were able to get tested AND the tests were positive.
 * 

```clojure
(defn test-percent-positive
  "A percentage for how many tests are positive?"
  [data]
  (try
  (when data
    {(:state data) (* 100.0 (/ (read-string (:positive data)) (read-string (:total data))))}
    )
  (catch Exception _)))

;; US states with highest (+) test percentage (resource: MiamiHeral data 20200404)
(def covid19pp (sort-map-by-value (apply merge (map #(percent-positive %) covid19))))

(pprint covid19pp)
{"Michigan" 51.72707716036855,
 "New Jersey" 44.2869205813075,
 "Oklahoma" 42.900564481111594,
 "New York" 37.95654644615169,
 "Connecticut" 24.55535571542766,
 "Georgia" 23.49120113381363,
 "Mississippi" 22.22222222222222,
 "Indiana" 19.271096159237448,
 "Louisiana" 19.19470593717961,
 "Colorado" 18.90716324588827,
 "Illinois" 18.53146853146853,
 "Massachusetts" 16.5210762046949,
 "Alabama" 15.78893231845299,
 "Rhode Island" 13.87858676556705,
 "District of Columbia" 13.55659025787966,
 "Pennsylvania" 13.55550189165258,
 "Maryland" 11.64204305614183,
 "Idaho" 11.42051860202931,
 "California" 11.287974683544299,
 "Florida" 10.71816283924843,
 "Virginia" 10.58668771375954,
 "South Carolina" 10.5204529983291,
 "Nevada" 10.41838700798238,
 "Texas" 9.558137866724051,
 "Kansas" 8.764489680520215,
 "Ohio" 8.630618892508144,
 "Washington" 8.433516144263248,
 "Delaware" 8.264462809917356,
 "Tennessee" 8.105393905758609,
 "Missouri" 7.8725782414307,
 "Wisconsin" 7.871876157931575,
 "Arkansas" 7.537534470432029,
 "Vermont" 7.4407039020658,
 "Iowa" 7.394477943509997,
 "Arizona" 7.169780731974224,
 "New Hampshire" 7.106198183971575,
 "Maine" 6.625766871165643,
 "North Carolina" 6.623836951705804,
 "Nebraska" 5.846153846153846,
 "Wyoming" 5.646258503401361,
 "Kentucky" 5.336501412792191,
 "Oregon" 5.156590570150281,
 "Utah" 5.138568129330253,
 "Montana" 4.325573716361235,
 "South Dakota" 3.9096801170813302,
 "West Virginia" 3.722318203235433,
 "Minnesota" 3.2566970735130227,
 "New Mexico" 3.166581371545548,
 "North Dakota" 2.983787512935495,
 "Alaska" 2.609707446808511,
 "Hawaii" 2.598143020035836}
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
     (let [total (read-string (:total data))]
     {(:state data) (* 100.0 (/ (- total (read-string (:positive data))) total))})
     (catch Exception _)
     ))

(pprint covid19unk)
{"Hawaii" 97.40185697996417,
 "Alaska" 97.3902925531915,
 "North Dakota" 97.01621248706451,
 "New Mexico" 96.83341862845445,
 "Minnesota" 96.74330292648699,
 "West Virginia" 96.27768179676457,
 "South Dakota" 96.09031988291868,
 "Montana" 95.67442628363877,
 "Utah" 94.86143187066975,
 "Oregon" 94.84340942984973,
 "Kentucky" 94.66349858720781,
 "Wyoming" 94.35374149659864,
 "Nebraska" 94.15384615384616,
 "North Carolina" 93.3761630482942,
 "Maine" 93.37423312883436,
 "New Hampshire" 92.89380181602841,
 "Arizona" 92.83021926802579,
 "Iowa" 92.60552205649,
 "Vermont" 92.5592960979342,
 "Arkansas" 92.46246552956798,
 "Wisconsin" 92.12812384206843,
 "Missouri" 92.1274217585693,
 "Tennessee" 91.8946060942414,
 "Delaware" 91.73553719008264,
 "Washington" 91.56648385573675,
 "Ohio" 91.36938110749186,
 "Kansas" 91.23551031947979,
 "Texas" 90.44186213327595,
 "Nevada" 89.58161299201763,
 "South Carolina" 89.4795470016709,
 "Virginia" 89.41331228624047,
 "Florida" 89.28183716075156,
 "California" 88.7120253164557,
 "Idaho" 88.5794813979707,
 "Maryland" 88.35795694385817,
 "Pennsylvania" 86.44449810834742,
 "District of Columbia" 86.44340974212034,
 "Rhode Island" 86.12141323443295,
 "Alabama" 84.21106768154701,
 "Massachusetts" 83.4789237953051,
 "Illinois" 81.46853146853147,
 "Colorado" 81.09283675411173,
 "Louisiana" 80.8052940628204,
 "Indiana" 80.72890384076254,
 "Mississippi" 77.77777777777779,
 "Georgia" 76.50879886618637,
 "Connecticut" 75.44464428457233,
 "New York" 62.0434535538483,
 "Oklahoma" 57.099435518888406,
 "New Jersey" 55.7130794186925,
 "Michigan" 48.27292283963145}
```