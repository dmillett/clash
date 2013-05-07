(defproject clash "0.11-SNAPSHOT"
  :description "A clojure library that encapsulates shell functionality for commands
  like 'grep' and 'cut'. This is useful for search or indexing large text files."
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :aot [clash]
  :jvm-opts ["-Xms256m" "-Xmx512m"]
  :repl-options {:init (do
                         (load-file "src/clash/tools.clj")
                         (load-file "src/clash/interact.clj")
                         (load-file "test/clash/interact_test.clj")
                         (use 'clash.tools)
                         (use 'clash.interact)

                         (defn load-local-resource
                           [logfile]
                           (str (System/getProperty "user.dir") logfile))

                         (println "Loaded 'clash' resources")
                         )} )