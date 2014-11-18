(defproject where-was-that-photo-taken "0.1.0-SNAPSHOT"
  :description "Where was that photo taken"
  :url "http://wherewasthatphototaken.com"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]
                 [lib-noir "0.7.6"]
                 [image-resizer "0.1.6"]
                 [clj-aws-s3 "0.3.10"]
                 [selmer "0.7.2"]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/data.json "0.2.5"]
                 [environ "1.0.0"]
                 [ring/ring-jetty-adapter "1.2.2"]]
  :plugins [[lein-ring "0.8.10"] [lein-environ "0.4.0"]]
  :ring {:handler where-was-that-photo-taken.handler/app
         :init where-was-that-photo-taken.handler/init
         :destroy where-was-that-photo-taken.handler/destroy}
  :aot :all
  :java-source-paths ["src/main/java"]
  :min-lein-version "2.0.0"
  :uberjar-name "where-was-that-photo-taken.jar"
  :profiles {
    :production
    {
      :ring
      {
        :open-browser? false, 
        :stacktraces? false, 
        :auto-reload? true
        :auto-refesh? true}}
    :dev {
    :dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.1"]]
    :env {
      :db-uri "mongo db uri"
      :s3-access-key "your key"
      :s3-secret-key "your secret"
      }
    }
   }
  })
