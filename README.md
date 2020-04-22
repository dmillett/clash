# Example Usage: COVID-19 (SARS-CoV-2)
  - from April 06, 2020 --> April 21, 2020

 * [Mortality](#mortality)
   - guesses at this point, but I tried to ball-park it
   - mortality driven by symptoms lasting 2+ weeks? (need data)
   - Higher mortality in northern states, why? Sunlight? Air pollution? Temperature? all?
 * [Symptoms](#symptoms)
   - Symptoms seem to persist for 4 - 6 weeks based on myself and some people I've talked to (need more data)
   - Average time unil severe symptoms?
   - Depletion of nutrients (Vitamin C, D, Glutathione, etc) as a function of virus time (days/weeks)?
   - Flu symptoms are typically 3 - 5 days and up to 2 weeks for most symptoms
 * [Completed Tests](#completed-tests)
   - Some hospitals, clinics etc will test for influenza, but it seems only for a small percentage of people
     this does nothing to reduce uncertainty
 * [Percent Positive Tests](#percent-positives)
 * [Percent Unknown Tests](#percent-uknown)
   - Many people with symptoms test negative, no data from CDC on false (-) estimates by test type
 * [Questions](#questions)

## Gather data 

For analyzing data, it is good to have a schema that has breadth and quantity. Finding Covid19 data that includes
the number of tests and positive results is helpful. Most sites just include the number of positive deaths and tests.
However, in new viral pandemics, this still leaves a lot of uncertainty about viral spread, test limitations and health
for the population.

There are more data resources from **Johns Hopkins** and **Tableau**, but finding CSVs that include more test data
than "active", "recovered", "fatal", location takes more effort. Lately, the compiled data from **Worldmeter** is
copy and pasted locally, before getting cleaned and parsed for use.

Initially, I grabbed a sample data from the Miami Herald that I did not include directly, but posted sample output.

 * [Illinois](https://dph.illinois.gov/covid19)
 * [Virginia](http://www.vdh.virginia.gov/coronavirus/)
 * [Worldmeter Coronavirus Data](https://www.worldometers.info/coronavirus/country/us/)
 
 * [Worldmeter Daily Reports](#wm-daily-reports)
   - [Parsing CSV Data](#wm-parsing-data)
   - [Percentages & Ratios]("#wm-percentages-and-ratios")
   - [Sort Criteria](#wm-sort-criteria)
   - [Test Imbalances](#wm-test-imbalance")
   - [Mortality](#mortality)
   - [Summaries & Gradients](#wm-summaries-and-gradients)
 
## Visualization (todo)

 * visualization (incanter, oz, etc) 
 
<a name="questions"/></a> 
## Questions? 

- Why are people with symptoms testing negative for SARS-COV-2 (covid19)?
 - Do they have an Influenza strain? 
 - Air pollution sensitivities?
 - Is the tissue (throat via nasal/mouth) swabbed location always going to have viral proteins?
   - If not, what about sputum/phlegm?
   - How long would the virus remain in the throat? 3 days? 4 days?
 - What are the test processing errors?
   - PCR processing familiarity? Abbott's "ID NOW Platform"
 - What is the false negative rate? 30% 40%?    
 
### Worldometer

Copy and paste table view into CSV. See **clash.example.covid19_worldmeter.clj**
 
#### Daily Reports

To help establish trends for some of these metrics, use **(wm-daily-workflow)** and **(wm-daily-sorts**. 

* *Since I had to calculate the percentage based on the death count per million and death counts are increasing, the population
shows an increase as well. Population is used consistently within a given day.*

<a name="wm-daily-reports"/></a>
```clojure
;; Get the percentage data for all files in a directory (days)
(def daily-data (wm-daily-workflow "/media/dave/storage/dev/clash/test/resources/worldmeter" maxkeys))
(sort (keys daily_data))
("us_20200406" "us_20200407"... "us_20200421")

(def daily_reports (wm-daily-sorts daily_data))
(keys daily_sorts)
(:daily_tests_deaths :daily_relatives :dates :combined :combined_gradients :combined_tests_deaths :combined_relatives)

;; 3, 5, 10 day averages for each field
(def avg-3-5-10 [{:fx #(mean (take 3 %)) :name "3-day-avg"} {:fx #(mean (take 5 %)) :name "5-day-avg"} {:fx #(mean (take 10 %)) :name "10-day-avg"}])
(def daily-avgs (combined-functions (:combined daily_sorts) avg-3-5-10-all))

;; "first" means after April 6
(pprint (select-keys (get daily-avgs "New York") death_percents))
{:death_test_positive_percent 
 {3-day-avg-from (us_20200406) 3.8694022, 
  5-day-avg-from (us_20200406) 4.1069884, 
  10-day-avg-from (us_20200406) 4.612323, 
  3-day-avg-until (us_20200420) 7.4138265, 
  5-day-avg-until (us_20200420) 7.3368526, 
  10-day-avg-until (us_20200420) 6.227255, 
  avg 5.520499,
  data [3.606840716819794 3.8550679851668734 4.146297901052451 4.375743015652863 4.550992701238121 4.762509384798834 4.954729034131405 5.139659093813089 5.333714055030696 5.397674331929485 7.12031052440782 7.322473509410091 7.33111794259068 7.401654430354145 7.508707069585155]}, 

:death_test_percent {
  :3-day-avg-from (us_20200406) 1.6045978, 
  :5-day-avg-from (us_20200406) 1.6991495, 
  :10-day-avg-from (us_20200406) 1.896054, 
  :3-day-avg-until (us_20200420) 2.9705217, 
  :5-day-avg-until (us_20200420) 2.9650779, 
  :10-day-avg-until (us_20200420) 2.5290182, 
  :avg 2.2523954, 
  :data [1.483116227311408 1.614136412023831 1.716540737718162 1.8048826583645978 1.877071443100375 1.956324549866207 2.033141176037314 2.102195640494442 2.1705202717457723 2.2026113472696442 2.925284110000563 2.9885402365222613 2.9622886953256486 2.962974957696076 2.9863014130858336]}, 

:death_population_percent { 
  :3-day-avg-from (us_20200406) 0.028060937, 
  :5-day-avg-from (us_20200406) 0.03203763, 
  :10-day-avg-from (us_20200406) 0.0417539, 
  :3-day-avg-until (us_20200420) 0.093275756, 
  :5-day-avg-until (us_20200420) 0.08984892, 
  :10-day-avg-until (us_20200420) 0.07065954, 
  :avg 0.057785574, 
  :data [0.02425340001268232 0.0279794408964513 0.031949972907768666 0.03602184839537116 0.03998349890583147 0.043974264223701834 0.04783778073475467 0.05125783808853935 0.05522454865247565 0.05905641549374063 0.08209517739510871 0.08732215902623687 0.09007431602571994 0.09326852823811987 0.09648441261157141]}
}
```

<a name="mortality"/></a>
#### Mortality rate

Given the testing issues & constraints (unknown false negatives, % of population tested), it is hard to get a realistic
value for the mortality rate for covid19. It is likely between the death rate for the entire population and death
rate for people tested as a percent of the population. 

So for New York, it's at least 0.06% of the population or for Michigan 0.02% now. New Jersey is right in the middle. 
These numbers will increase as the virus progresses, but it's probably not horribly unrealistic to expect the northern
states to have a higher mortality rate. The northern states seem to be faring worse, so there is probably a significant 
temperature factor as well. 

```clojure
;; Last 3 days
(def last_3_days (sort-map-by-value (:combined_averages daily_sorts) :ksubpath [:death_population_percent] :ksubset ["3-day-avg-until"]))
(take 5 (keys last_3_days))
("New York" "New Jersey" "Connecticut" "Louisiana" "Massachusetts")
(take 10 (keys last_3_days))
("New York" "New Jersey" "Connecticut" "Louisiana" "Massachusetts" "Michigan" "District Of Columbia" "Rhode Island" "Illinois" "Pennsylvania")

(def last_3_days_dt (sort-map-by-value (:combined_averages daily_sorts) :ksubpath [:death_test_percent] :ksubset ["3-day-avg-until"]))
("Northern Mariana Islands" "New York" "New Jersey" "Michigan" "Connecticut" "Massachusetts" "Colorado" "Louisiana" "Indiana" "Illinois")

;; Last 5, 10 days (Note Washington state is moving down the list, while Pennsylvania is moving up
(def last_5_days (sort-map-by-value (:combined_averages daily_sorts) :ksubpath [:death_population_percent] :ksubset ["5-day-avg-until"]))
(take 5 (keys last_5_days))
("New York" "New Jersey" "Connecticut" "Louisiana" "Michigan")

(take 10 (keys last_10_days))
("New York" "New Jersey" "Connecticut" "Louisiana" "Michigan" "Massachusetts" "District Of Columbia" "Rhode Island" "Illinois" "Washington")

(def last_10_days_dt (sort-map-by-value (:combined_averages daily_sorts) :ksubpath [:death_test_percent] :ksubset ["10-day-avg-until"]))
("Northern Mariana Islands" "New York" "New Jersey" "Michigan" "Connecticut" "Louisiana" "Indiana" "Georgia" "Colorado" "Massachusetts")

;; From April 6, 2020
(def avg (sort-map-by-value (:combined_averages daily_sorts) :ksubpath [:death_population_percent] :ksubset ["avg"]))
(take 5 (keys avg))
("New York" "New Jersey" "Louisiana" "Connecticut" "Michigan")
(take 10 (keys avg))
("New York" "New Jersey" "Louisiana" "Connecticut" "Michigan" "Massachusetts" "District Of Columbia" "Rhode Island" "Washington" "Illinois")
```

```clojure
;; New York
(select-keys (get (:combined_death daily_sorts) "New York") death_percents)
{:death_test_positive_percent [3.606840716819794 3.8550679851668734 4.146297901052451 4.375743015652863 4.550992701238121 4.762509384798834 4.954729034131405 5.139659093813089 5.333714055030696 5.397674331929485], 
:death_test_percent [1.483116227311408 1.614136412023831 1.716540737718162 1.8048826583645978 1.877071443100375 1.956324549866207 2.033141176037314 2.102195640494442 2.1705202717457723 2.2026113472696442],
:death_population_percent [0.02425340001268232 0.0279794408964513 0.031949972907768666 0.03602184839537116 0.03998349890583147 0.043974264223701834 0.04783778073475467 0.05125783808853935 0.05522454865247565 0.05905641549374063]}

;; Louisiana
(select-keys (get (:combined_death daily_sorts) "Louisiana") death_percents)
{:death_test_positive_percent [3.443868971547723 3.574060427413412 3.8285378743394007 3.83963244544112 3.9214667843972375 4.027180973318677 4.0786598689002185 4.206318995051389 4.7076865879728595 5.024828026058039],
:death_test_percent [0.7402480987768557 0.7795860960417922 0.8009237648330589 0.8076485003278915 0.8181621153012569 0.8316566063044937 0.807342976596665 0.8178294215059534 0.8554153788991911 0.9046322419788729], 
:death_population_percent [0.010978619567245121 0.012479616090985409 0.01398092761310399 0.0150529555255926 0.01618897400072218 0.01728265612553298 0.01801182476295688 0.018955650853208002 0.021721565976951553 0.02365161154747584]}
```

<a name="wm-summaries-and-gradients"/></a>
##### Summaries

**Relative Sorting**
```clojure
;; Top 10 states in terms of testing and death
(keys (take 10 (:combined_relatives daily_sorts)))
("New York" "New Jersey" "California" "Michigan" "Louisiana" "Florida" "Washington" "Massachusetts" "Illinois" "Pennsylvania")

;; As a combination of tests and deaths with higher percentages
(keys (take 10 (:combined_tests_deaths daily_sorts)))
("New York" "Northern Mariana Islands" "Louisiana" "New Jersey" "Michigan" "Oklahoma" "Washington" "Connecticut" "Massachusetts" "District Of Columbia")

;; These states are increasing the fastest Sorted by highest gradient for 'deaths per test' (linear growth = 1.0)
(def dtgradients (ct/sort-map-by-value (:combined_gradients daily_sorts) :ksubpath [:death_test_percent :gradients] :datafx +values))
(keys (take 10 dtgradients))

;; April 21st
("Virginia" "Puerto Rico" "New Jersey" "Tennessee" "New York" "North Carolina" "Rhode Island" "Nebraska" "District Of Columbia" "Arkansas")

;; April 15th
("Virginia" "Puerto Rico" "Tennessee" "New Jersey" "North Carolina" "District Of Columbia" "Rhode Island" "Pennsylvania" "Ohio" "Kentucky")

;; April 13th
("North Carolina" "Rhode Island" "New Jersey" "Ohio" "Kentucky" "Iowa" "Oklahoma" "Tennessee" "Arizona" "Massachusetts")
```
**Gradients**
Even though New York vs North Carolina has two orders of magnitude more deaths associated with covid19, its deaths relative to
test percentage is actually decreasing (gradient < 1.0). However, North Carolina has a low test rate relative to its
increasing deaths. For how long will it increase?

```clojure
;; New York's slope is on a downward trend still
;; Virginia jumped to the late gradient lead with a large number of deaths today

(get-in (:combined daily_sorts) ["New York" :death_count])
[4758 5489 6268 7067 7844 8627 9385 10056 10834 11586]

(get-in (:combined daily_sorts) ["New York" :death_test_percent])
[1.483116227311408 1.614136412023831 1.716540737718162 1.8048826583645978 1.877071443100375 1.956324549866207 2.033141176037314 2.102195640494442 2.1705202717457723 2.2026113472696442]

(get-in (:combined_gradients daily_sorts) ["New York" :death_test_percent :gradients])
[0.13102018471242305 0.78159198080127 0.8626776266280934 0.8171520859807081 1.0978589964620042 0.9692569705573332 0.8989520615408266 0.9894310496571795 0.4696853087406463]
```

##### Data dump through April 15, 2020

This is highly dependent on the states with the most testing and deaths. So the order will not change much.

```clojure
(pr-percentages (take 5 (:combined_relatives daily_reports)) :focus relative_pr_focus)

(
New York:
 :population:[19617868 19617976 19618170 19618649 19618093 19618293 19618385 19618463],
**:death_count:[4758 5489 6268 7067 7844 8627 9385 10056],
 :death_population_percent:[0.02425340001268232 0.0279794408964513 0.031949972907768666 0.03602184839537116 0.03998349890583147 0.043974264223701834 0.04783778073475467 0.05125783808853935],
 :death_test_percent:[1.483116227311408 1.614136412023831 1.716540737718162 1.8048826583645978 1.877071443100375 1.956324549866207 2.033141176037314 2.102195640494442],
 :death_test_positive_percent:[3.606840716819794 3.8550679851668734 4.146297901052451 4.375743015652863 4.550992701238121 4.762509384798834 4.954729034131405 5.139659093813089],
**:death_ratio_relative_max:[1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0],
**:test_count:[320811 340058 365153 391549 417885 440980 461601 478357],
 :test_positive_count:[131916 142384 151171 161504 172358 181144 189415 195655],
 :test_positive_percent:[41.11953767171325 41.87050444335966 41.39935862501472 41.24745561858158 41.24531868815583 41.07759989115153 41.034356511359384 40.90146062459627],
 :test_unknowns_percent:[58.880462328286754 58.12949555664034 58.60064137498529 58.75254438141842 58.75468131184417 58.92240010884847 58.965643488640616 59.098539375403725],
 :test_population_percent:[1.635300023427622 1.733400020471021 1.861300009124195 1.995800016606648 2.130100005133017 2.247800050697581 2.3529000985555126 2.438300084976076],
 :test_positive_population_percent:[0.6724278091788567 0.7257833325925162 0.770566265864757 0.8232167260854709 0.8785665354935366 0.9233423111786534 0.9654974147973955 0.9973003491659872],
 :test_unknown_population_percent:[0.9628722142487655 1.007616687878505 1.090733743259438 1.172583290521177 1.25153346963948 1.3244577395189279 1.387402683758118 1.440999735810089],
**:test_ratio_relative_max:[1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0],
**:test_positive_ratio_relative_max:[1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0],
 
New Jersey:
 :population:[8881883 8881885 8881550 8881550 8882168 8882131 8881841 8881751],
**:death_count:[1003 1232 1504 1700 1932 2183 2350 2443],
 :death_population_percent:[0.01129265044360526 0.01387092942545417 0.01693398111816068 0.01914080312558056 0.021751446268523628 0.0245774353024066 0.02645847859694854 0.027505837531360648],
 :death_test_percent:[1.126561236409381 1.2971971276349321 1.499112891972171 1.6944760082132249 1.701857773314659 1.8162455384256988 1.8542628318933212 1.861773066400445],
 :death_test_positive_percent:[2.4409832075930877 2.773775216138329 3.170520901406075 3.331569561212691 3.539239393273247 3.7540197073137183 3.799514955537591 3.782670630496717],
**:death_ratio_relative_max:[0.210802858343842 0.2244488977955912 0.2399489470325463 0.2405546908164709 0.2463029066802652 0.2530427726903906 0.2503995737879595 0.24293953858393],
**:test_count:[89032 94974 100326 100326 113523 120193 126735 131219],
 :test_positive_count:[41090 44416 47437 51027 54588 58151 61850 64584],
 :test_positive_percent:[46.151945367957595 46.76648345863078 47.28285788330044 50.86119251240954 48.08541000502101 48.38135332340486 48.80261963940506 49.218482079576894],
 :test_unknowns_percent:[53.848054632042405 53.23351654136922 52.717142116699556 49.138807487590455 51.91458999497899 51.618646676595134 51.197380360594934 50.78151792042311],
 :test_population_percent:[1.002400054132665 1.069300041601529 1.129600126104115 1.129600126104115 1.278100121501868 1.3532000372433148 1.426900121269903 1.4774001207644751],
 :test_positive_population_percent:[0.46262712535168504 0.5000740270787113 0.5341072222753911 0.5745280947582347 0.6145796836988446 0.6546964911911343 0.6963646388175605 0.7271539136821107],
 :test_unknown_population_percent:[0.5397729287809804 0.5692260145228181 0.5954929038287236 0.5550720313458799 0.6635204378030229 0.6985035460521806 0.7305354824523429 0.7502462070823647],
**:test_ratio_relative_max:[0.2775216560529408 0.2792876509301354 0.2747505840017746 0.2562284669351729 0.271660863634732 0.2725588462061772 0.2745552977571539 0.2743118633154736],
**:test_positive_ratio_relative_max:[0.3114860972133782 0.3119451623777953 0.3137969584113355 0.3159488309887062 0.3167128882906509 0.3210208452943515 0.326531689676108 0.3300912320155376],
 
California:
 :population:[39144440 39150558 39150558 39152298 39150558 39150558 39145496 39145496],
**:death_count:[380 447 498 559 584 630 681 727],
 :death_population_percent:[9.707636640094992E-4 0.0011417461789433498 0.001272012521507356 0.001427757829182849 0.001491677334458426 0.001609172466967137 0.001739663740625486 0.0018571740667176631],
 :death_test_percent:[0.32608788926741783 0.3108484005563282 0.3463143254520167 0.3418960244648318 0.3542335151004167 0.3821354700569564 0.3348082595870206 0.3574237954768928],
 :death_test_positive_percent:[2.3721830326487297 2.548896618577864 2.644715878916622 2.79905863502078 2.771318749110236 2.84129346502503 2.924378408554129 3.011723766518911],
**:death_ratio_relative_max:[0.07986548970155528 0.08143559846966661 0.07945118059987237 0.07910004245082779 0.0744518103008669 0.07302654456937523 0.07256259989344699 0.07229514717581544],
**:test_count:[116533 143800 143800 163500 164863 164863 203400 203400],
 :test_positive_count:[16019 17537 18830 19971 21073 22173 23287 24139],
 :test_positive_percent:[13.746320784670441 12.19541029207232 13.094575799721838 12.214678899082571 12.782128191286098 13.44934885329031 11.44886922320551 11.8677482792527],
 :test_unknowns_percent:[86.25367921532956 87.80458970792768 86.90542420027816 87.78532110091743 87.2178718087139 86.55065114670968 88.5511307767945 88.1322517207473],
 :test_population_percent:[0.2977000054158394 0.3673000011902768 0.3673000011902768 0.41760000907226436 0.42110000066921144 0.42110000066921144 0.5196000071119293 0.5196000071119293],
 :test_positive_population_percent:[0.04092279772044255 0.044793742147940775 0.04809637706824 0.051008500190716774 0.05382554189904522 0.05663520811121007 0.059488325298011305 0.06166482090302292],
 :test_unknown_population_percent:[0.2567772076953968 0.322506259042336 0.3192036241220368 0.36659150888154757 0.3672744587701662 0.3644647925580013 0.46011168181391804 0.4579351862089064],
**:test_ratio_relative_max:[0.3632450258875163 0.4228690399872963 0.3938075272556983 0.4175722578783243 0.3945176304485684 0.373855957186267 0.4406402932402659 0.4252054427969069],
**:test_positive_ratio_relative_max:[0.121433336365566 0.1231669288684122 0.1245609276911577 0.1236563800277393 0.1222629642952459 0.1224053791458729 0.1229416888841961 0.1233753290230252],
 
Michigan:
 :population:[9958206 9956874 9956874 9957807 9957705 9957296 9957296 9957296],
**:death_count:[727 845 959 1076 1281 1392 1487 1602],
 :death_population_percent:[0.007300511758844916 0.008486599308176442 0.009631536966320957 0.01080559203447104 0.012864410022188851 0.01397969890620908 0.01493377318500926 0.016088705206714748],
 :death_test_percent:[1.5891405088747048 1.6788524199316541 1.905348486052611 2.0353346196042827 1.778080062184221 1.831241613387008 1.9562185913121268 2.1075065119583227],
 :death_test_positive_percent:[4.221589919284594 4.454401686874012 4.713457190602575 5.003720238095237 5.622613352060747 5.801692160213396 6.0353924831561 6.249268578115856],
**:death_ratio_relative_max:[0.1527952921395544 0.1539442521406449 0.1529993618379068 0.1522569690108957 0.1633095359510454 0.1613538889532862 0.158444326052211 0.1593078758949881],
**:test_count:[45748 50332 50332 52866 72044 76014 76014 76014],
 :test_positive_count:[17221 18970 20346 21504 22783 23993 24638 25635],
 :test_positive_percent:[37.64317565795226 37.68974012556624 40.42358737979814 40.67642719328113 31.62372994281273 31.563922435340857 32.41245033809561 33.72405083274134],
 :test_unknowns_percent:[62.356824342047744 62.31025987443376 59.57641262020186 59.32357280671887 68.37627005718727 68.43607756465914 67.58754966190439 66.27594916725866],
 :test_population_percent:[0.4594000164286619 0.5055000193835937 0.5055000193835937 0.5309000264817344 0.7235000434337028 0.7634000234601844 0.7634000234601844 0.7634000234601844],
 :test_positive_population_percent:[0.1729327551569028 0.19052164364036342 0.2043412420404235 0.2159511627409529 0.228797699871607 0.24095899127634648 0.2474366534850425 0.2574494119688719],
 :test_unknown_population_percent:[0.2864672612717592 0.3149783757432303 0.3011587773431702 0.3149488637407815 0.49470234356209586 0.522441032183838 0.5159633699751418 0.5059506114913125],
**:test_ratio_relative_max:[0.1426010953489749 0.1480100453452058 0.1378381116956454 0.1350175840060886 0.1724014980197901 0.172375164406549 0.1646746865799684 0.1589064234452511],
**:test_positive_ratio_relative_max:[0.1305451954273932 0.1332312619395438 0.1345893061499891 0.1331484049930652 0.1321841747989649 0.1324526343682374 0.1300741757516564 0.1310214408014106],
 
Louisiana:
 :population:[4663610 4663605 4663496 4663536 4663668 4663635 4663603 4663517],
**:death_count:[512 582 652 702 755 806 840 884],
 :death_population_percent:[0.010978619567245121 0.012479616090985409 0.01398092761310399 0.0150529555255926 0.01618897400072218 0.01728265612553298 0.01801182476295688 0.018955650853208002],
 :death_test_percent:[0.7402480987768557 0.7795860960417922 0.8009237648330589 0.8076485003278915 0.8181621153012569 0.8316566063044937 0.807342976596665 0.8178294215059534],
 :death_test_positive_percent:[3.443868971547723 3.574060427413412 3.8285378743394007 3.83963244544112 3.9214667843972375 4.027180973318677 4.0786598689002185 4.206318995051389],
**:death_ratio_relative_max:[0.1076082387557797 0.1060302423027874 0.1040204211869815 0.09933493703127211 0.09625191228964813 0.09342761098875622 0.08950452850293021 0.08790771678599842],
**:test_count:[69166 74655 81406 86919 92280 96915 104045 108091],
 :test_positive_count:[14867 16284 17030 18283 19253 20014 20595 21016],
 :test_positive_percent:[21.49466500881936 21.81233674904561 20.91983391887576 21.034526398140798 20.863675769397492 20.651086003198678 19.79431976548609 19.442876835259177],
 :test_unknowns_percent:[78.50533499118065 78.18766325095439 79.08016608112423 78.9654736018592 79.13632423060251 79.34891399680131 80.2056802345139 80.55712316474082],
 :test_population_percent:[1.483100001929835 1.600800239299855 1.745600296429974 1.86380034377348 1.978700027531977 2.078100022836264 2.23100036602601 2.317800063771613],
 :test_positive_population_percent:[0.3187873771606116 0.3491719388756123 0.3651766829005536 0.3920415753196716 0.4128295581932505 0.42915022294840827 0.4416113464203535 0.45064701168667337],
 :test_unknown_population_percent:[1.164312624769224 1.251628300424243 1.380423613529421 1.471758768453808 1.565870469338727 1.648949799887856 1.789389019605657 1.867153052084939],
**:test_ratio_relative_max:[0.2155973454775553 0.2195360791394409 0.2229366868134727 0.2219875417891503 0.2208263038874331 0.219771871740215 0.2254002915938224 0.2259630359752235],
**:test_positive_ratio_relative_max:[0.1127005063828497 0.1143667827845825 0.1126538820276376 0.1132046265107985 0.1117035472678959 0.1104866846265954 0.1087295092785682 0.1074135595819172],
)
``` 

Looking at a sort by death and test percentages yields a similar story.

```clojure
(pr-percentages (take 5 (:combined_tests_deaths daily_reports)) :focus death_test_percents)

(
New York:
 :population:[19617868 19617976 19618170],
 :death_count:[4758 5489 6268],
 :death_population_percent:[0.02425340001268232 0.0279794408964513 0.031949972907768666],
**:death_test_percent:[1.483116227311408 1.614136412023831 1.716540737718162],
 :death_test_positive_percent:[3.606840716819794 3.8550679851668734 4.146297901052451],
 :death_ratio_relative_max:[1.0 1.0 1.0],
 :test_count:[320811 340058 365153],
 :test_positive_count:[131916 142384 151171],
**:test_positive_percent:[41.11953767171325 41.87050444335966 41.39935862501472],
**:test_unknowns_percent:[58.880462328286754 58.12949555664034 58.60064137498529],
**:test_population_percent:[1.635300023427622 1.733400020471021 1.861300009124195],
 :test_positive_population_percent:[0.6724278091788567 0.7257833325925162 0.770566265864757],
 :test_unknown_population_percent:[0.9628722142487655 1.007616687878505 1.090733743259438],
 :test_ratio_relative_max:[1.0 1.0 1.0],
 :test_positive_ratio_relative_max:[1.0 1.0 1.0],
 
Northern Mariana Islands:
 :population:[nil nil nil],
 :death_count:[1 2 2],
 :death_population_percent:[nil nil nil],
**:death_test_percent:[3.03030303030303 6.0606060606060606 4.444444444444444],
 :death_test_positive_percent:[12.5 25.0 18.18181818181818],
 :death_ratio_relative_max:[2.1017234E-4 3.6436509E-4 3.1908104E-4],
 :test_count:[33 33 45],
 :test_positive_count:[8 8 11],
**:test_positive_percent:[24.24242424242424 24.24242424242424 24.44444444444444],
**:test_unknowns_percent:[75.75757575757575 75.75757575757575 75.55555555555557],
**:test_population_percent:[nil nil nil],
 :test_positive_population_percent:[nil nil nil],
 :test_unknown_population_percent:[nil nil nil],
 :test_ratio_relative_max:[1.028643E-4 9.704227E-5 1.2323601E-4],
 :test_positive_ratio_relative_max:[6.0644652E-5 5.6186087E-5 7.276528E-5],
 
Louisiana:
 :population:[4663610 4663605 4663496],
 :death_count:[512 582 652],
 :death_population_percent:[0.010978619567245121 0.012479616090985409 0.01398092761310399],
**:death_test_percent:[0.7402480987768557 0.7795860960417922 0.8009237648330589],
 :death_test_positive_percent:[3.443868971547723 3.574060427413412 3.8285378743394007],
 :death_ratio_relative_max:[0.10760824 0.10603024 0.104020424],
 :test_count:[69166 74655 81406],
 :test_positive_count:[14867 16284 17030],
**:test_positive_percent:[21.49466500881936 21.81233674904561 20.91983391887576],
**:test_unknowns_percent:[78.50533499118065 78.18766325095439 79.08016608112423],
**:test_population_percent:[1.483100001929835 1.600800239299855 1.745600296429974],
 :test_positive_population_percent:[0.3187873771606116 0.3491719388756123 0.3651766829005536],
 :test_unknown_population_percent:[1.164312624769224 1.251628300424243 1.380423613529421],
 :test_ratio_relative_max:[0.21559735 0.21953608 0.22293669],
 :test_positive_ratio_relative_max:[0.11270051 0.114366785 0.11265388],
 
New Jersey:
 :population:[8881883 8881885 8881550],
 :death_count:[1003 1232 1504],
 :death_population_percent:[0.01129265044360526 0.01387092942545417 0.01693398111816068],
**:death_test_percent:[1.126561236409381 1.2971971276349321 1.499112891972171],
 :death_test_positive_percent:[2.4409832075930877 2.773775216138329 3.170520901406075],
 :death_ratio_relative_max:[0.21080285 0.2244489 0.23994894],
 :test_count:[89032 94974 100326],
 :test_positive_count:[41090 44416 47437],
**:test_positive_percent:[46.151945367957595 46.76648345863078 47.28285788330044],
**:test_unknowns_percent:[53.848054632042405 53.23351654136922 52.717142116699556],
**:test_population_percent:[1.002400054132665 1.069300041601529 1.129600126104115],
 :test_positive_population_percent:[0.46262712535168504 0.5000740270787113 0.5341072222753911],
 :test_unknown_population_percent:[0.5397729287809804 0.5692260145228181 0.5954929038287236],
 :test_ratio_relative_max:[0.27752167 0.27928764 0.2747506],
 :test_positive_ratio_relative_max:[0.3114861 0.31194517 0.31379697],
 
Michigan:
 :population:[9958206 9956874 9956874],
 :death_count:[727 845 959],
 :death_population_percent:[0.007300511758844916 0.008486599308176442 0.009631536966320957],
**:death_test_percent:[1.5891405088747048 1.6788524199316541 1.905348486052611],
 :death_test_positive_percent:[4.221589919284594 4.454401686874012 4.713457190602575],
 :death_ratio_relative_max:[0.15279528 0.15394425 0.15299936],
 :test_count:[45748 50332 50332],
 :test_positive_count:[17221 18970 20346],
**:test_positive_percent:[37.64317565795226 37.68974012556624 40.42358737979814],
**:test_unknowns_percent:[62.356824342047744 62.31025987443376 59.57641262020186],
**:test_population_percent:[0.4594000164286619 0.5055000193835937 0.5055000193835937],
 :test_positive_population_percent:[0.1729327551569028 0.19052164364036342 0.2043412420404235],
 :test_unknown_population_percent:[0.2864672612717592 0.3149783757432303 0.3011587773431702],
 :test_ratio_relative_max:[0.1426011 0.14801005 0.13783811],
 :test_positive_ratio_relative_max:[0.1305452 0.13323127 0.1345893],
)
```

<a name="wm-test-imbalance"/></a>
##### Test Imbalances

Looking at just test and death metrics only bumps Florida close to the top even though they have far fewer deaths
than the five states that look at more factors.

Why don't all of the other states have the test capability of Florida? Many states in the Midwest are only able
to test around half the rate as Florida, yet have a lot more deaths.

```clojure
;; Sort on these keys
(def tests [:test_count :death_count :test_positive_percent])

(pr-percentages (take 5 (sort-map-by-value (:combined daily_reports) :ksubset tests :datafx ++values )) :focus tests)
(
New York:
 :population:[19617868 19617976 19618170],
**:death_count:[4758 5489 6268],
 :death_population_percent:[0.02425340001268232 0.0279794408964513 0.031949972907768666],
 :death_test_percent:[1.483116227311408 1.614136412023831 1.716540737718162],
 :death_test_positive_percent:[3.606840716819794 3.8550679851668734 4.146297901052451],
 :death_ratio_relative_max:[1.0 1.0 1.0],
**:test_count:[320811 340058 365153],
 :test_positive_count:[131916 142384 151171],
**:test_positive_percent:[41.11953767171325 41.87050444335966 41.39935862501472],
 :test_unknowns_percent:[58.880462328286754 58.12949555664034 58.60064137498529],
 :test_population_percent:[1.635300023427622 1.733400020471021 1.861300009124195],
 :test_positive_population_percent:[0.6724278091788567 0.7257833325925162 0.770566265864757],
 :test_unknown_population_percent:[0.9628722142487655 1.007616687878505 1.090733743259438],
 :test_ratio_relative_max:[1.0 1.0 1.0],
 :test_positive_ratio_relative_max:[1.0 1.0 1.0],
 
Florida:
 :population:[20599444 20597109 20596951],
**:death_count:[254 296 323],
 :death_population_percent:[0.001233042988927274 0.001437094885500679 0.001568193272878107],
 :death_test_percent:[0.2015105356689515 0.2119296336338056 0.22342118005118627],
 :death_test_positive_percent:[1.8636730501137282 2.007187902624263 2.0575869537520703],
 :death_ratio_relative_max:[0.053383775 0.053926032 0.05153159],
**:test_count:[126048 139669 144570],
 :test_positive_count:[13629 14747 15698],
**:test_positive_percent:[10.81254760091394 10.55853482161396 10.858407691775609],
 :test_unknowns_percent:[89.18745239908607 89.44146517838604 89.14159230822439],
 :test_population_percent:[0.611900010505138 0.6781000187938997 0.7019000045200865],
 :test_positive_population_percent:[0.06616197990586542 0.07159742660972468 0.07621516407938243],
 :test_unknown_population_percent:[0.5457380305992725 0.606502592184175 0.625684840440704],
 :test_ratio_relative_max:[0.39290422 0.41072112 0.39591622],
 :test_positive_ratio_relative_max:[0.10331575 0.10357203 0.10384267],
 
California:
 :population:[39144440 39150558 39150558],
**:death_count:[380 447 498],
 :death_population_percent:[9.707636640094992E-4 0.0011417461789433498 0.001272012521507356],
 :death_test_percent:[0.32608788926741783 0.3108484005563282 0.3463143254520167],
 :death_test_positive_percent:[2.3721830326487297 2.548896618577864 2.644715878916622],
 :death_ratio_relative_max:[0.07986549 0.0814356 0.07945118],
**:test_count:[116533 143800 143800],
 :test_positive_count:[16019 17537 18830],
**:test_positive_percent:[13.746320784670441 12.19541029207232 13.094575799721838],
 :test_unknowns_percent:[86.25367921532956 87.80458970792768 86.90542420027816],
 :test_population_percent:[0.2977000054158394 0.3673000011902768 0.3673000011902768],
 :test_positive_population_percent:[0.04092279772044255 0.044793742147940775 0.04809637706824],
 :test_unknown_population_percent:[0.2567772076953968 0.322506259042336 0.3192036241220368],
 :test_ratio_relative_max:[0.36324504 0.42286903 0.39380753],
 :test_positive_ratio_relative_max:[0.12143334 0.123166926 0.12456093],
 
Washington:
 :population:[7294244 7294244 7294066],
**:death_count:[381 403 431],
 :death_population_percent:[0.005223296615797333 0.005524904294399803 0.005908912806656808],
 :death_test_percent:[0.4169630642954856 0.4410396716826265 0.4681068282775624],
 :death_test_positive_percent:[4.576026903675234 4.641787606542271 4.613573110682937],
 :death_ratio_relative_max:[0.08007566 0.07341956 0.06876197],
**:test_count:[91375 91375 92073],
 :test_positive_count:[8326 8682 9342],
**:test_positive_percent:[9.111901504787962 9.501504787961695 10.14629696002085],
 :test_unknowns_percent:[90.88809849521205 90.49849521203829 89.85370303997915],
 :test_population_percent:[1.252700074195489 1.252700074195489 1.262300066931119],
 :test_positive_population_percent:[0.1141447969110987 0.11902535752848412 0.1280767133173733],
 :test_unknown_population_percent:[1.13855527728439 1.1336747166670051 1.1342233536137458],
 :test_ratio_relative_max:[0.28482503 0.26870418 0.2521491],
 :test_positive_ratio_relative_max:[0.063115925 0.060975954 0.061797567],
 
New Jersey:
 :population:[8881883 8881885 8881550],
**:death_count:[1003 1232 1504],
 :death_population_percent:[0.01129265044360526 0.01387092942545417 0.01693398111816068],
 :death_test_percent:[1.126561236409381 1.2971971276349321 1.499112891972171],
 :death_test_positive_percent:[2.4409832075930877 2.773775216138329 3.170520901406075],
 :death_ratio_relative_max:[0.21080285 0.2244489 0.23994894],
**:test_count:[89032 94974 100326],
 :test_positive_count:[41090 44416 47437],
**:test_positive_percent:[46.151945367957595 46.76648345863078 47.28285788330044],
 :test_unknowns_percent:[53.848054632042405 53.23351654136922 52.717142116699556],
 :test_population_percent:[1.002400054132665 1.069300041601529 1.129600126104115],
 :test_positive_population_percent:[0.46262712535168504 0.5000740270787113 0.5341072222753911],
 :test_unknown_population_percent:[0.5397729287809804 0.5692260145228181 0.5954929038287236],
 :test_ratio_relative_max:[0.27752167 0.27928764 0.2747506],
 :test_positive_ratio_relative_max:[0.3114861 0.31194517 0.31379697],
)
```

<a name="wm-parsing-data"/></a>
#### Parsing data 
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

<a name="wm-percentages-and-ratios"/></a>
#### Calculate some percentages, relative ratios**

```clojure
;; Find maximum values for these keys
(def maxkeys [:total_pos :deaths :cases_million :deaths_million :test_count :tests_million])
(def maximums (wm-maximums covid maxkeys))
(def population-percentages (wm-percentages covid maximums))

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
```

<a name="wm-sort-criteria"/></a>
#### Sort by relative Max

This starts to identify the 5 states with the highest positive test and death rates. One goal is a more even distribution of
test data across the US.

```clojure
(defn +values [values] (apply + (filter identity values)))
(defn *values [values] (apply * (filter identity values)))

;; Sort by relative maximums
(def relative_to_max [:test_ratio_relative_max :test_positive_ratio_relative_max :death_ratio_relative_max])
(def sorted_relative1 (sort-map-by-value population-percentages :ksubset relative_to_max :datafx *values))

;; Specialized printer to highlight specific fields
(pr-percentages (take 5 sort_relative_20200406wm) :focus (conj relative_to_max :death_count :test_count))
(New York:
 :population:19617868,
**:death_count:4758**,
 :death_population_percent:0.02425340001268232,
 :death_test_percent:1.483116227311408,
 :death_test_positive_percent:3.606840716819794,
**:death_ratio_relative_max:1.0,
**:test_count:320811,
 :test_positive_count:131916,
 :test_positive_percent:41.11953767171325,
 :test_unknowns_percent:58.880462328286754,
 :test_population_percent:1.635300023427622,
 :test_positive_population_percent:0.6724278091788567,
 :test_unknown_population_percent:0.9628722142487655,
**:test_ratio_relative_max:1.0,
**:test_positive_ratio_relative_max:1.0,
 New Jersey:
 :population:8881883,
**:death_count:1003,
 :death_population_percent:0.01129265044360526,
 :death_test_percent:1.126561236409381,
 :death_test_positive_percent:2.4409832075930877,
**:death_ratio_relative_max:0.21080285,
**:test_count:89032,
 :test_positive_count:41090,
 :test_positive_percent:46.151945367957595,
 :test_unknowns_percent:53.848054632042405,
 :test_population_percent:1.002400054132665,
 :test_positive_population_percent:0.46262712535168504,
 :test_unknown_population_percent:0.5397729287809804,
**:test_ratio_relative_max:0.27752167,
**:test_positive_ratio_relative_max:0.3114861,
 California:
 :population:39144440,
**:death_count:380,
 :death_population_percent:9.707636640094992E-4,
 :death_test_percent:0.32608788926741783,
 :death_test_positive_percent:2.3721830326487297,
**:death_ratio_relative_max:0.07986549,
**:test_count:116533,
 :test_positive_count:16019,
 :test_positive_percent:13.746320784670441,
 :test_unknowns_percent:86.25367921532956,
 :test_population_percent:0.2977000054158394,
 :test_positive_population_percent:0.04092279772044255,
 :test_unknown_population_percent:0.2567772076953968,
**:test_ratio_relative_max:0.36324504,
**:test_positive_ratio_relative_max:0.12143334,
 Florida:
 :population:20599444,
**:death_count:254,
 :death_population_percent:0.001233042988927274,
 :death_test_percent:0.2015105356689515,
 :death_test_positive_percent:1.8636730501137282,
**:death_ratio_relative_max:0.053383775,
**:test_count:126048,
 :test_positive_count:13629,
 :test_positive_percent:10.81254760091394,
 :test_unknowns_percent:89.18745239908607,
 :test_population_percent:0.611900010505138,
 :test_positive_population_percent:0.06616197990586542,
 :test_unknown_population_percent:0.5457380305992725,
**:test_ratio_relative_max:0.39290422,
**:test_positive_ratio_relative_max:0.10331575,
 Louisiana:
 :population:4663610,
**:death_count:512,
 :death_population_percent:0.010978619567245121,
 :death_test_percent:0.7402480987768557,
 :death_test_positive_percent:3.443868971547723,
**:death_ratio_relative_max:0.10760824,
**:test_count:69166,
 :test_positive_count:14867,
 :test_positive_percent:21.49466500881936,
 :test_unknowns_percent:78.50533499118065,
 :test_population_percent:1.483100001929835,
 :test_positive_population_percent:0.3187873771606116,
 :test_unknown_population_percent:1.164312624769224,
**:test_ratio_relative_max:0.21559735,
**:test_positive_ratio_relative_max:0.11270051,
)
```

<a name="miami-herald"/></a>
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