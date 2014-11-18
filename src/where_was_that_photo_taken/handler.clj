(ns where-was-that-photo-taken.handler
  (:require [compojure.route :as route]
            [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [where-was-that-photo-taken.routes.home :refer [home-routes]]
            [where-was-that-photo-taken.routes.upload :refer :all]
            [noir.util.middleware :as noir-middleware]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(defn init []
  (println "where-was-that-photo-taken is starting"))

(defn destroy []
  (println "where-was-that-photo-taken is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app (noir-middleware/app-handler
           [upload-routes
            app-routes]))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (handler/site #'app) {:port port :join? false})))


