(ns where-was-that-photo-taken.views.layout
  (:require [hiccup.page :refer [html5 include-css]]))

(defn common [& body]
  (html5
    [:head
     [:title "Welcome to where-was-that-photo-taken"]
     (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
     (include-css "/css/screen.css")]
    [:body body]))
