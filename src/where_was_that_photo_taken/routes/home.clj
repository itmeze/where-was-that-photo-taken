(ns where-was-that-photo-taken.routes.home
  (:require [compojure.core :refer :all]
            [where-was-that-photo-taken.views.layout :as layout]))

(defn home []
  (layout/common [:h1 "Hello World!"]))

(defroutes home-routes
  (GET "/" [] (home)))
