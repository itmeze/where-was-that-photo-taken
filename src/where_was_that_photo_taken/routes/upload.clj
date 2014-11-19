(ns where-was-that-photo-taken.routes.upload
  (:refer-clojure :exclude [sort find])
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.form :refer :all]
            [hiccup.util :refer [url-encode]]
            [hiccup.element :refer [image]]
            [noir.session :as session]
            [noir.util.route :refer [restricted]]
            [clojure.java.io :as io]
            [ring.util.response :as resp]
            [where-was-that-photo-taken.views.layout :as layout]
            [where-was-that-photo-taken.helpers.filehelpers :refer :all]
            [where-was-that-photo-taken.helpers.date :refer :all]
            [where-was-that-photo-taken.helpers.cookies :refer :all]
            [selmer.parser :as selmer]
            [monger.core :as mg]
            [monger.query :refer :all]
            [monger.collection :as mc]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [where-was-that-photo-taken.helpers.db :as db])
  (:import [java.io File]
           [com.mongodb MongoOptions ServerAddress]
           [org.bson.types.ObjectId]
           (java.util Calendar Date UUID TimeZone Locale)
           (java.text SimpleDateFormat)
           (org.bson.types ObjectId)))

(def gallery-path "galleries")

(def s3-cred { :access-key (env :s3-access-key "key") :secret-key (env :s3-secret-key "your secret")})

(defn geocode-url [latlng]
  (format "https://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&key=AIzaSyBrVe0kS1yCrrRhv9QjDlSpBYKeYrr3vHM" (:latitude latlng) (:longitude latlng)))

(defn is-owner [el]
  (= (get-user-tracker) (:user el)))

(defn flash-map []
  (reduce #(assoc %1 %2 (session/flash-get %2)) {} [:success :info :warning :error]))

(defn render-file
  [path data]
  (selmer/render-file path (merge data {:context {:user (get-user-tracker) :flash (flash-map)} })))

(defn get-location
  [latlng]
  (let [loc (slurp (geocode-url latlng))]
    (-> (json/read-str loc :key-fn #(keyword %)) :results first :formatted_address)))

(defn upload-page [info]
  (let [recent (db/with-conn
    (fn [conn]
      (doall (with-collection (:db conn) "photos"
              (find {})
              (fields [:path :loc :resolved :geo-tag])
              (sort (array-map :date -1))
              (limit 10)))))]
  (render-file "templates/homepage.html" {:info info :recent recent})))

(defn details-page [id]
  (let [el (db/get-by-id "photos" id)]
    (if (:resolved el)
      (render-file "templates/details-resolved.html" el)
      (render-file "templates/details-unresolved.html" el))))

(defn my-photos []
  (render-file "templates/my-photos.html" { :photos (db/get-by-owner "photos" (get-user-tracker)) }))

(defn handle-mark-as-unresolved [id]
  (let [el (db/get-by-id "photos" id)]
    (if (is-owner el)
      (do
        (db/with-conn (fn [conn] (mc/update-by-id (:db conn) "photos" (ObjectId. id) (assoc el :resolved false))))
        (session/flash-put! :success "We have marked it as unresolved")))
    (resp/redirect-after-post (format "/details/%s" id))))

(defn handle-mark-as-resolved [id loc]
  (let [el (db/get-by-id "photos" id)]
    (if (is-owner el)
      (do
        (db/with-conn (fn [conn] (mc/update-by-id (:db conn) "photos" (ObjectId. id) (assoc el :resolved true :loc loc))))
        (session/flash-put! :success "Changes were saved!")))
    (resp/redirect (format "/details/%s" id))))

(defn handle-remove [id]
  (let [el (db/get-by-id "photos" id)]
    (if (is-owner el)
      (do
        (db/with-conn (fn [conn] (mc/update-by-id (:db conn) "photos" (ObjectId. id) (assoc el :visible false))))
        (session/flash-put! :success "Photo was removed so others do not see it. You can always see it on your photos")))
    (resp/redirect (format "/details/%s" id))))

(defn handle-show [id]
  (let [el (db/get-by-id "photos" id)]
    (if (is-owner el)
      (do
        (db/with-conn (fn [conn] (mc/update-by-id (:db conn) "photos" (ObjectId. id) (assoc el :visible true))))
        (session/flash-put! :success "Photo is now visible to all")))
    (resp/redirect (format "/details/%s" id))))


(defn handle-mark-as-visible
  [id]
  (let [el (db/get-by-id "photos" id)]
    (if (is-owner el)
      (do
        (session/flash-put! :success "Great, now others can help you figure out where was that photo taken!")
        (db/with-conn (fn [conn]
                        (mc/update-by-id (:db conn) "photos" (ObjectId. id) {:visible true})))
        (resp/redirect-after-post (format "/details/%s" id)))
      (resp/redirect (format "/details/%s" id)))))

(defn is-valid-geotag
  [geo-tag]
  (not= (:latitude geo-tag) 0.0)) ;kind of tricky...

(defn handle-upload [{:keys [filename] :as file}]
  (println "handling upload")
  (cond
    (empty? filename)
    (upload-page "please select file for upload")
    (not (is-img file))
    (upload-page "uploaded file is not an image")
    :else
    (do
      (println "final gallery upload")
      (upload-file file gallery-path)
      (let
        [resized-file-path (resize-file (with-file (file-path gallery-path filename)) 480)
         s3-path (upload-to-s3 (with-file resized-file-path) filename s3-cred)
         geo-tag (geo-tag-path (file-path gallery-path filename))
         loc (get-location geo-tag)]
        (io/delete-file resized-file-path)
        (io/delete-file (file-path gallery-path filename))
        (let [info {:geo-tag geo-tag :path s3-path :loc loc}
              final (assoc info :_id (ObjectId.) :date (Date.) :user (get-or-set-user-tracker))]
          (db/with-conn (fn [conn-obj]
          (if (is-valid-geotag geo-tag)
            (do
              (mc/insert (:db conn-obj) "photos" (assoc final :resolved true :visible true))
              (session/flash-put! :success "Great! We have managed to resolve pictures's location")
              (resp/redirect-after-post (format "/details/%s" (:_id final))))
            (do
              (mc/insert (:db conn-obj) "photos" (assoc final :resolved false :visible false :geo-tag nil))
              (session/flash-put! :warning "Sorry, we couldn't resolve pictures's location")
              (resp/redirect-after-post (format "/details/%s" (:_id final))))))))))))

(defn serve-file [file-name]
  (resp/file-response (str gallery-path File/separator file-name)))

(defroutes upload-routes
  (GET "/" [info] (upload-page info))
  (GET "/details/:id" [id] (details-page id))
  (POST "/help-me/:id" [id] (handle-mark-as-visible id))
  (POST "/mark-as-unresolved/:id" [id] (handle-mark-as-unresolved id))
  (POST "/mark-as-resolved/:id" [id loc] (handle-mark-as-resolved id loc))
  (POST "/remove/:id" [id] (handle-remove id))
  (POST "/show/:id" [id] (handle-show id))
  (POST "/upload" [file] (handle-upload file))
  (GET "/my-photos" [] (my-photos))
  (GET "/img/:file-name" [file-name] (serve-file file-name)))