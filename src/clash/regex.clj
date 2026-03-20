(ns clash.regex)

(defn pattern
  ([regexes] (pattern regexes true))
  ([regexes as_text?]
    (when (and regexes (coll? regexes))
      (let [pattern_text (apply str regexes)]
        (if (and pattern_text (not as_text?))
          (re-pattern as_text?)
          pattern_text)
        )
      ) ) )

(defn map-regex
  "What is the regex equivalent for this character?

  What characters to escape? ['?' '}' '{' ]

  This function should be passed in so any correlation function
  could be used.
  "
  [ch & {:keys [general? literals] :or {general? true literals #{\? \| \{ \} \[ \] \, \: \. \_}}}]
  (when ch
    (cond
      (Character/isDigit ch) (if general? "\\d" (str ch))
      (Character/isLetter ch) (if general? "\\w" (str ch))
      (Character/isSpaceChar ch) "\\s"
      (some #{ch} literals) (str "\\" ch)
      :else "."
      ) ) )

(defn group-regex
  [rex & {:keys [mergex] :or {mergex #{"\\w" "\\d"}}}]
  (reduce
    (fn [result ch]
      (let [current (last result)
            lc (last current)]
        (if (and (some #{lc} mergex) (some #{ch} mergex))
          (-> result pop (conj (conj current ch)))
          (conj result [ch])
          ) ) )
    []
    rex) )

(defn simplify-regex
  [full_regex & {:keys [mergex] :or {mergex #{"\\w" "\\d"}}}]
  (let [fx_length1 (fn [x] (str "{" x "}"))
        fx_length2 (fn [x g] (str "[" (apply str g) "]" (fx_length1 x)))
        grouped (if (empty? mergex)
                  (partition-by identity full_regex)
                  (group-regex full_regex :mergex mergex)
                  )
        ]
    (reduce
      (fn [result ch]
        (let [l (count ch)]
          (cond
            (= l 1) (conj result (first ch))
            (or (empty? mergex) (= 1 (count (set ch)))) (conj result (str (first ch) (fx_length1 l)))
            (not (empty? mergex)) (conj result (fx_length2 l (set ch)))
            ) ) )
      []
      grouped)
    ) )

(defn build-regexes
  [text & {:keys [test? as_text? max mergex] :or {test? false as_text? true max 100 mergex #{"\\w" "\\d"}}}]
  (when text
    (let [delta (- max (count text))
          trailer (if (pos? delta) "" (str ".{" (Math/abs delta) "}"))
          full_regex (reduce (fn [r ch] (conj r (map-regex ch))) [] (take max text))
          ]
      (tap> full_regex)
      (pattern (conj (simplify-regex full_regex :mergex mergex) trailer) as_text?)
    ) ) )