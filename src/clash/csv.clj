(ns clash.csv
  (:require [clojure.string :as s]
            [clash.tools :as ct]))

(defn- record-field-count
  "How many define fields are in the defrecord definition?"
  [rec]
  (when rec
    (count (filter
             #(not (s/includes? (str %) "__"))
             (.getFields rec))
      ) ) )

;; todo: return map with {:values, :remainders}
(defn- ensure-record-row
  "Ensure vector is truncated or expanded (with nil) until it is
  the desired 'size'"
  [values size & {:keys [fill] :or {fill nil}}]
  (let [sdiff (- size (count values))]
    (cond
      (pos? sdiff) (concat values (repeat sdiff fill))
      (neg? sdiff) (take size values)
      :default values
      )))

(defn- resultdata
  "Add to :result data with conj or set to :result data to vector with data"
  [result data]
  (if (:result result)
    (conj (:result result) data)
    [data]))

(defn keyfx
  "Create keyfx from the first row in a CSV stream (or similar). It takes a list of arguments
  and converts them to a function with map keys or defrecord fields. Default behavior is a function
  that takes a vector/list of values OR a defrecord function that takes a vector/list of va

  rowdata - a vector of header fields to create map keys or defrecord fields from
  recname - what the defrecord type should be, if null, then hashmap is created
  keywords? - if map keys should be strings or keywords, defaults to false (strings), cleanup field names

  ;; map
  ((keyfx [a b c]) [1 2 3]) -> {\"a\" 1 \"b\" 2 \"c\" 3}

  ; defrecord
  ((keyfx [a b c] :recname \"Foo\") [4 5 6]) -> user.Foo{:a 4 :b 5 :c 6}

  todo: hashmap keys as :keyword instead of string
  "
  [rowdata & {:keys [recname keywords?] :or {recname nil keywords? false}}]
  (when (not (empty? rowdata))
    (if recname
      (let [_ (ct/create-record recname rowdata)
            fieldcount (count rowdata)
            recfx (ct/eval-str (str "->" recname))]
        (fn [values] (apply recfx (ensure-record-row values fieldcount))) )
      (fn [values] (zipmap (if keywords? (map keyword rowdata) rowdata) values))
      ) ) )

(defn stateful-join
  "Is a stateful transducing join/merge function that is designed
  for CSV header and data. If the first line in a CSV data stream is
  the header row, then it will be parsed into map keys or a defrecord.
  Each subsequent line will be converted into a map structure. If there
  are extra columns for some rows, then that extra data is dropped.

  header? - Whether or not to parse a header from the first row
  recname - (paired with header?) will create a defrecord instead of zipmap
  key-clean - cleaning up header fields so they are compatible with Java field syntax requirements
  keywords? - map keys as keywords instead of strings (subject to keyword naming restrictions, use recleanfx)
  "
  [& {:keys [header? recname kclean keywords?] :or {header? false recname nil kclean nil keywords? false}}]
  (fn
    ([] {})
    ([result] (if result result {}))
    ([result input]
      (let [keyfn (:keyfx result)]
        (try
          (cond
            (and keyfn input) (assoc result :result (resultdata result (keyfn input)))
            (and header? input) (assoc result :keyfx (keyfx
                                                       (if kclean (map kclean input) input)
                                                       :recname recname
                                                       :keywords? keywords?))
            input (assoc result :result (resultdata result (if kclean (map kclean input) input)))
            :default result
            )
         (catch Exception e (println "(stateful-join) error on:" input "\n" e)))
       ) ) ) )

(defn csv-parse1
  "Just a simple string split on ','"
  [textrow]
  (when textrow
    (s/split (s/trim textrow) #",")))

(defn csv-parse2
  "Parse a simple string on ',' and convert numbers and leave
  everything else a string. Uses (re-matches #\"[\\d\\.\\-]+\" text)
  to identify numbers."
  [textrow]
  (when textrow
    (let [values (s/split (s/trim textrow) #",")
          numpat #"[\d\.\-]+"]
      (map
        (fn [text]
          (if (or (= "nil" text) (re-matches numpat text))
            (read-string text)
            text))
        values)
      ) ) )

(defn clean-keys
  "Remove trailing newline and whitespace. Replace '-' with '_'
  todo: this should capture all Java field syntax naming requirements
  "
  [text]
  (when (not (empty? text))
    (-> text s/trim (s/replace #"[\-\/]+" "_") (s/replace #"[\s\+]+" ""))))