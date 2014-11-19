(ns where-was-that-photo-taken.helpers.filehelpers
  (:require [clojure.java.io :as io]
            ;[image-resizer.resize :refer :all]
            ;[image-resizer.scale-methods :refer :all]
            ;[image-resizer.format :as format]
            ;[image-resizer.core :refer :all]
            [aws.sdk.s3 :as s3]
            [noir.io :as noir-io]
            [mikera.image.core :refer :all])
  (:import [java.io File FileInputStream FileOutputStream ByteArrayOutputStream ByteArrayInputStream]
           (main.java JpegGeoTagReader)
           (org.apache.commons.io IOUtils)
           (java.util UUID)
           (java.net URLDecoder)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.awt Graphics RenderingHints)
           (java.awt.geom AffineTransform)))

(defn file-path [path & [filename]]
  (URLDecoder/decode (str path File/separator filename) "utf-8"))

(def image-content-types ["image/jpeg" "image/pjpeg" "image/png" "image/svg+xml" "image/gif"])

(defn is-img [file]
  (some #(= (:content-type file) %) image-content-types))

(defn with-file
  [path]
  (io/file path))

(defn resize-to-byte-array
  [file]
  (let [ort-bi (ImageIO/read file)
        res-bi (ImageResizer/getScaledImage ort-bi (int 640))
        out (ByteArrayOutputStream.)]
    (ImageIO/write res-bi "jpg" out)
    (ByteArrayInputStream. (.toByteArray out))))

(defn resize-and-upload-to-s3
  ([file cred]
   (resize-and-upload-to-s3 file (.getName file) cred))
  ([file new-name cred]
   (let [key (str (UUID/randomUUID) "/" new-name)
         ;resized (resize file 640 640)
         ;resized-stream (format/as-stream resized "jpg")
         resized-stream (resize-to-byte-array file)
         bucket "where-was-that-photo-taken"]
    (s3/put-object cred bucket key resized-stream {:content-type "image/jpeg" :content-length (.available resized-stream)} (s3/grant :all-users :read))
    (str "https://s3.amazonaws.com/" bucket "/" key))))

(defn geo-tag-file
  [file]
  (let [geo-tag-reader (new JpegGeoTagReader)
       	geo-info (.readMetadata geo-tag-reader file)]
       	{:latitude (.getLatitude geo-info) :longitude (.getLongitude geo-info)}))

(defn geo-tag-path
  [path]
  (geo-tag-file (io/file path)))

;(defn resize-file-to-stream
;  [file]
;  (format/as-stream (resize-to-width file 480) "jpg"))

;(defn resize-file
;  "returns file path of resized file"
;  [file new-width]
;  (let [resized (resize-to-width file new-width)]
;    (format/as-file resized (.getAbsolutePath file))))

(defn upload-file
  [ifile to-folder]
  (noir-io/upload-file to-folder ifile))