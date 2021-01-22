(defproject clash "1.5.1"
  :description "A clojure library that applies customizable structures to text files
   and quick analysis via filter groups, maps, etc. This is useful for quickly searchin
   or indexing large text files before spending proportionally more effort on Hadoop or
   Spark."
  :url "https://github.com/dmillett/clash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html" }
  :scm {:name "git" :url "https://github.com/dmillett/clash" }
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/math.combinatorics "0.1.6"]
                 [incanter "1.9.3"]
                 [cheshire "5.10.0"]
                 [org.clojure/data.json "1.0.0"]]
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "1.0.700"]]}}
  :plugins [[lein-kibit "0.1.3"]
            [jonase/eastwood "0.2.3"]
            [lein-ancient "0.6.15"]]
  :jvm-opts ["-Xms256m" "-Xmx256m"]
  ;:global-vars {*warn-on-reflection* true}
  :repl-options {:init (do
                         (require '[clojure.spec.alpha :as s])
                         (require '[clash.shape :as cs])
                         (require '[clash.tools :as ct])
                         (require '[clash.core :as cc])
                         (require '[clash.pivot :as cp])
                         (require '[clash.text_tools :as ctt])
                         (require '[clash.command :as ccmd])
                         (require '[clash.csv :as ccsv])
                         (require '[clash.example.web_shop_example :as clex])
                         (require '[clash.example.web_shop_example_test :as clext])
                         ;(use 'clash.example.covid19_miamiherald)
                         ;(use 'clash.example.covid19_worldmeter)
                         (defn load-local-resource
                           [logfile]
                           (str (System/getProperty "user.dir") logfile))

                         (println "Loaded 'clash' resources")
                         )} )
