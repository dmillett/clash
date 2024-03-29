(defproject clash "1.5.4"
  :description "A clojure library that applies customizable structures to text files
   and quick analysis via filter groups, maps, etc. This is useful for quickly searchin
   or indexing large text files before spending proportionally more effort on Hadoop or
   Spark."
  :url "https://github.com/dmillett/clash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html" }
  :scm {:name "git" :url "https://github.com/dmillett/clash" }
  ;;:repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/math.combinatorics "0.2.0"]
                 ;[incanter "1.9.3"]
                 [cheshire "5.11.0"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-commons/clj-yaml "1.0.26"]]
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "1.82.1306"]]}}
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
                         (defn load-local-resource
                           [logfile]
                           (str (System/getProperty "user.dir") logfile))

                         (println "Loaded 'clash' resources")
                         )} )
