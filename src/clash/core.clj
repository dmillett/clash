(ns clash.core)
(use '[clojure.java.io :only(reader writer)])
(use '[clojure.string :only(split)])

;; Linux/Unix "/bin/sh", "-c"
;; Mac
;; Windows: throw exception

; Move to tools.clj
(defn stb
  "Sh*t the bed message."
  [message]
   (throw (RuntimeException. (str message))) )

; Move to tools.clj
(defn str-contains?
  "Does a string contain a given text." 
  [text, chars]
  (if-not (or (empty? text) (empty? chars))
          (. text contains chars)) )  

;; todo: macro?
(defn prefix-command
  "Build a command array for linux, prefixing with the following
  system commands: \"/bin/sh\", \"-c\", 'command'. This will
  enable multiple commands to execute via 'pipe'. "
  [command]
  (if (str-contains? command "|")
      ; linux, solaris, pretty much non microsoft
      (into-array (list "/bin/sh" "-c" command))
       command) )


(defn jproc
  "Get a Java Process for a Runtime system execution."
  [command]
  (let [updated (prefix-command command)]
    (. (Runtime/getRuntime) exec updated)) )


(defn jproc-instream
  "Get the input stream for a Java Process."
  [command]
  (. (jproc command) getInputStream))

 
(defn jproc-reader
  "Get a clojure reader from a java InputStream."
  [command]
  (if-not (nil? command)
    (reader (jproc-instream command))) )


;; todo: make this into one method with writer or console dump??

; It's pretty slow dumping to the console, but useful for testing.
(defn jproc-and-dump
  "Execution a shell system command, via java process, and
  dump result to the console (slow). This is handled via
  clojure reader to a line-seq. "
  [command, delim]
  (let [proc (jproc command)]
    (with-open [rdr (jproc-reader command)]
      (doseq [line (line-seq rdr)]
        (println (str line delim))))) )
  
(defn jproc-and-write
  "Execute a System command, via java Process, and capture
  the InputStream via clojure reader into a sequenc.e
  Write the resulting output to a file (useful for grep)."
  [command, output, delim]
  (let [process (jproc command)]
      (with-open [rdr (jproc-reader command)
                  wrt (writer output :append true)]
           (doseq [line (line-seq rdr)]
              (.write wrt (str line delim))))) )

