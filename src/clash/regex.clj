;   Copyright (c) David Millett. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clash.regex
  (:require [clash.text_tools :as ctt]))

(def char_literals
  "A map of character Sets that defines groups of character literals. These
  might be used for ignoring escaped characters, etc."
  {
   :basic_punct #{\? \| \{ \} \[ \] \, \: \. \_}
   :basic_alphanum #{"\\w" "\\d"}
   })

(defn pattern-rex
  "Creates a pattern or regular expression text representation.
  "
  ([regexes] (pattern-rex regexes true))
  ([regexes as_text?]
   (tap> regexes)
    (when (and regexes (coll? regexes))
      (let [pattern_text (apply str regexes)]
        (if (and pattern_text (not as_text?))
          (re-pattern pattern_text)
          pattern_text)
        )
      ) ) )

(defn map-regex
  "What is the regex equivalent for this character?

  What characters to escape? ['?' '}' '{' ], see char_literals

  This function should be passed in so any correlation function
  could be used.
  "
  [ch & {:keys [general? literals] :or {general? true literals (:basic_punct char_literals)}}]
  (when ch
    (cond
      (Character/isDigit ch) (if general? "\\d" (str ch))
      (Character/isLetter ch) (if general? "\\w" (str ch))
      (Character/isSpaceChar ch) "\\s"
      (some #{ch} literals) (str "\\" ch)
      :else "."
      ) ) )

(defn group-regex
  "Group character classes consecutively based on
  :mergex character Set.
  "
  [rex & {:keys [mergex] :or {mergex (:basic_alphanum char_literals)}}]
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
  "Try to combine regex class characters according to what is specified
  in :mergex, which defaults to \\w, \\d. For example:
  [\\w \\w \\d \\w] --> [\\w\\d]{4}

  If :merge #{}, then it groups by identity.

  [\\w \\w \\d \\d \\w] --> \\w{2}\\d{2}\\w
  "
  [full_regex & {:keys [mergex] :or {mergex (:basic_alphanum char_literals)}}]
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

(defn build-regex
  "Create a regular expression that represents the 'text' passed in.

  as_text? - return the pattern or text representation of the pattern
  max - The maximum number of characters to create a regex for, add .{n} for the remaining
  mergex - Group these character classes together or by identity
  "
  [^String text & {:keys [as_text? max mergex] :or {as_text? true max 100
                                            mergex (:basic_alphanum char_literals)}}]
  (when text
    (let [delta (- max (count text))
          trailer (if (pos? delta) "" (str ".{" (Math/abs delta) "}"))
          full_regex (reduce (fn [r ch] (conj r (map-regex ch))) [] (take max text))
          regex (pattern-rex (conj (simplify-regex full_regex :mergex mergex) trailer) as_text?)
          ]
      (tap> full_regex)
      regex
    ) ) )