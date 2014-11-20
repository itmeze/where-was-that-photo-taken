(ns where-was-that-photo-taken.helpers.cookies
  (:import (java.util UUID Date TimeZone Locale)
           (java.text SimpleDateFormat))
  (:require [noir.cookies :as cookie]
            [where-was-that-photo-taken.helpers.date :refer :all]))


(defn to-cookie-expiry-string
  [date]
  (let [df (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss zzz" Locale/ROOT)]
    (do
      (.setTimeZone df (TimeZone/getTimeZone "GMT"))
      (.format df date))))

(defn get-or-set-user-tracker []
  (if (cookie/get :tracker)
    (cookie/get :tracker)
    (let [tracker (UUID/randomUUID)]
        (cookie/put! :tracker {:value tracker :path "/" :expires (to-cookie-expiry-string (addDays (Date.) 356))})
        (.toString tracker))))

(defn get-user-tracker []
  (cookie/get :tracker))

