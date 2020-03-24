(ns clash.specking
  (:require [clojure.spec.alpha :as spec]))

(defn explaining
  "Some explaining when a conformance goes astray."
  [fx value msg]
  (if-let [failure (= :clojure.spec.alpha/invalid (spec/explain fx value))]
    (str failure "," msg)
    value))