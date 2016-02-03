(defproject clash "1.1.1"
  :description "A clojure library that applies customizable structures to text files
   and quick analysis via filter groups, maps, etc. This is useful for quickly searchin
   or indexing large text files before spending proportionally more effort on Hadoop or
   Spark."
  :url "https://github.com/dmillett/clash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html" }
  :scm {:name "git" :url "https://github.com/dmillett/clash" }
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.1"]]
  :jvm-opts ["-Xms256m" "-Xmx256m"]
  ;:global-vars {*warn-on-reflection* true}
  :repl-options {:init (do
                         (load-file "src/clash/tools.clj")
                         (load-file "src/clash/core.clj")
                         (load-file "src/clash/text_tools.clj")
                         (load-file "test/clash/core_test.clj")
                         (use 'clash.tools)
                         (use 'clash.core)
                         (use 'clash.pivot)
                         (use 'clash.text_tools)
                         (defn load-local-resource
                           [logfile]
                           (str (System/getProperty "user.dir") logfile))

                         (println "Loaded 'clash' resources")
                         )} )
