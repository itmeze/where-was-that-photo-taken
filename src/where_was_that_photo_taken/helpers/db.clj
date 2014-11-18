(ns where-was-that-photo-taken.helpers.db
  (:require [monger.core :as mg]
            [monger.query :as query]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import 	(org.bson.types ObjectId)))


(def mg-uri (env :db-uri))

(defn with-conn 
	[f]
 	(let [conn-obj (mg/connect-via-uri mg-uri)
          res (f conn-obj)]
     	(mg/disconnect (:conn conn-obj))
      res))

(defn get-by-id
  [coll id]
  (with-conn (fn [conn]
  	(mc/find-one-as-map (:db conn) coll { :_id (ObjectId. id) }))))


(defn get-by-owner
  [coll owner]
  (if (nil? owner)
    {}
    (with-conn (fn [conn]
               (doall (mc/find-maps (:db conn) coll { :user owner }))))))
