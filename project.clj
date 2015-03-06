(defproject clash "0.11-SNAPSHOT"
  :description "A clojure library that encapsulates shell functionality for commands
  like 'grep' and 'cut'. This is useful for search or indexing large text files."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/math.combinatorics "0.0.8"]]
  :jvm-opts ["-Xms256m" "-Xmx512m"]
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init (do
                         (load-file "src/clash/tools.clj")
                         (load-file "src/clash/core.clj")
                         (load-file "test/clash/core_test.clj")
                         (use 'clash.tools)
                         (use 'clash.core)
                         (use 'clash.pivot)
                         (defn load-local-resource
                           [logfile]
                           (str (System/getProperty "user.dir") logfile))

                         (println "Loaded 'clash' resources")
                         )} )
