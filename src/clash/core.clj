(ns clash.core)
(use '[clojure.java.io :only(reader writer)])
(use '[clojure.string :only(split)])

(defn str-contains?
  "Does a string contain a given text." 
  [text, chars]
  (if-not (or (empty? text) (empty? chars))
          (. text contains chars)) )  

(defn jsystem-cmd-prefix
  "Build a command array for linux, prefixing with the following
  system commands: \"/bin/sh\", \"-c\", 'command'. This will
  enable multiple commands to execute via 'pipe'. "
  [command]
  (if (str-contains? command "|")
      (into-array (list "/bin/sh" "-c" command))
       command) )


(defn jproc
  "Get a Java Process for a Runtime system execution."
  [command]
  (let [updated (jsystem-cmd-prefix command)]
    (. (Runtime/getRuntime) exec updated)) )
 
(defn jproc-instream
  "Get the input stream for a Java Process."
  [command]
  (. (jproc command) getInputStream))

 
(defn jsystem-cmd
  "Execute a System command, via java Process, and capture
  the InputStream into a clojure reader for sequence functionality.
  Write the resulting output to a file (useful for grep)."
  [command, output]
  (let [process (jproc command)]
      (with-open [rdr (reader (jproc-instream command))
                  wrt (writer output :append true)]
           (doseq [line (line-seq rdr)]
              (.write wrt (str line "\n"))))) )

