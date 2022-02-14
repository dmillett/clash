;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns
    ^{:author "David Millett"
      :doc "Use performant shell commands like 'grep', 'cut', etc piped together on larger files."}
    clash.command
  (:require [clojure.string :as s]
            [clojure.spec.alpha :as spec]
            [clash.core :as cc])
  (:use [clojure.java.io :only (reader writer)]
        [clash.text_tools :refer :all]))

;; Linux/Unix "/bin/sh", "-c"
;; Mac
;; Windows: throw exception
;; Runtime.getRuntime.exec()
; String[] args
; string[] env
; target directory
(defn- build-command
  "Creates a string or sequence of command line arguments. If there is a '|' in the command line string or args,
  then the first elements of the sequence will be '/bin/sh' and '-c' to satisfy how Java passes commands
  to the underlying linux os.

  (build-command \"ls\") --> \"ls\"
  (build-command \"ls | grep md\" --> [\"/bin/sh\" \"-c\" \"ls | grep md\"] - typed array
  (build-command [\"ls\" \"|\" \"grep md\"] --> [\"/bin/sh\" \"-c\" \"ls | grep md\"] - typed array
  "
  [args]
  {pre (spec/valid? #(or (string? %) (coll? %)) args)}
  (cond
    (and (coll? args) (some #{"|"} args)) (into-array (list "/bin/sh" "-c" (apply str (interpose " " args))))
    (coll? args) (apply str (interpose " " args))
    (s/includes? args "|") (into-array (list "/bin/sh" "-c" args))
    :default args
    ) )

(defn jshell-stream
  "Create a reader for a command line string OR collection of arguments. The command
  line arguments can include pipe ('|') delimiters.

  cmdline - A string or list of command line arguments
  :sh (todo) What type of shell environment (default: /bin/sh)
  "
  [cmdline & {:keys [sh] :or {sh "/bin/sh"}}]
  {:pre (spec/valid? #(or (string? %) (coll? %)) cmdline)}
  (let [command (build-command cmdline)]
    (.getInputStream (.exec (Runtime/getRuntime) command))) )

(defn jshell
  "Use (transform-lines) to wrap output from a command line execution. This
  wraps clash.core/transform-lines (a transducer) and allows for some actions
  on the data from the input stream.

  The optional arguments and defaults mirror the underlying (transform-lines).
  "
  [args & {:keys [max tdfx joinfx initv] :or {max nil tdfx nil joinfx conj initv []}}]
  (let [instream (jshell-stream args)
        parser (fn [line] (s/trim line))]
    (cc/transform-lines instream parser :max max :tdfx nil :joinfx joinfx :initv initv)
    ) )


(defn pipe
  "Build a command array for linux, prefixing with the following
  system commands: \"/bin/sh\", \"-c\", 'command'. This will
  enable multiple commands to execute via 'pipe'. "
  [command]
  (if (s/includes? command "|")
    ; linux, solaris, pretty much non microsoft
    (into-array (list "/bin/sh" "-c" command))
    command) )

(defn ^{:deprecated "1.5.4"} jproc
  "Get a Java Process for a Runtime system execution."
  [command]
  (let [updated (pipe command)]
    (.exec (Runtime/getRuntime) updated)) )

(defn ^{:deprecated "1.5.4"}  jproc-instream
  "Get the input stream for a Java Process."
  [command]
  (.getInputStream (jproc command)))

 
(defn ^{:deprecated "1.5.4"}  jproc-reader
  "Get a clojure reader from a java InputStream."
  [command]
  (if-not (nil? command)
    (reader (jproc-instream command))) )


;; todo: make this into one method with writer or console dump??

; It's pretty slow dumping to the console, but useful for testing.
(defn ^{:deprecated "1.5.4"}  jproc-dump
  "Execution a shell system command, via java process, and
  dump result to the console (slow). This is handled via
  clojure reader to a line-seq. "
  [command, delim]
  (with-open [rdr (jproc-reader command)]
    (doseq [line (line-seq rdr)]
      (println (str line delim)))) )
  
(defn ^{:deprecated "1.5.4"}  jproc-write
  "Execute a System command, via java Process, and capture
  the InputStream via clojure reader into a sequenc.e
  Write the resulting output to a file (useful for grep)."
  [command, output, delim]
  (with-open [rdr (jproc-reader command)
              wrt (writer output :append true)]
    (doseq [line (line-seq rdr)]
      (.write wrt (str line delim)))) )


;; Explore using map and functions here??
;; Explore using multiple functions with @
(defmacro ^{:deprecated "1.5.4"}  with-jproc
  "A macro to combine clojure functions with a result
  from a shell command.

  Usage:
   (with-jproc command \":\" output last)
"
  [command delim output function]
  `(with-open [rdr# (jproc-reader ~command)
               wrt# (writer ~output)]
     (doseq [line# (line-seq rdr#)]
       (let [result# (~function line#)]
         (.write wrt# (str result# ~delim))))) )


(defmacro ^{:deprecated "1.5.4"}  with-jproc-dump
  "Execute a sh command like grep and pass the result to
  a function to work with. For example, pass the result
  of a grep to 'last': (last (grep bar \"foobar\")) --> 'r'

  Usage:
   (with-jproc-dump command2 \":\" last)
"
  [command delim function]
  `(with-open [rdr# (jproc-reader ~command)]
     (doseq [line# (line-seq rdr#)]
       (let [result# (~function line#)]
         (println (str "macro result? " result# ~delim))))) )

