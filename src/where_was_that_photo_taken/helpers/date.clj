(ns where-was-that-photo-taken.helpers.date
  (:import (java.util Calendar)))

(defn addDays
  [date num]
  (let [cal (Calendar/getInstance)]
    (do
      (.setTime cal date)
      (.add cal Calendar/DATE num)
      (.getTime cal))))
