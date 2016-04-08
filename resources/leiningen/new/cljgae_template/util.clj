(ns {{name}}.util
  (:require [clojure.tools.logging :as log]))


(defmacro try-with-default [default & forms]
  `(try 
    (do  ~@forms)
    (catch Exception ~'ex
      (do 
        (log/warn (str "Failed with " (type ~'ex) ": " (.getMessage ~'ex) ". Defaulting to " ~default))
        ~default) )))

; Blobstore support probably belongs in gcs.clj

(defn get-blobstore-service [] (BlobstoreServiceFactory/getBlobstoreService))

(defn gen-file-upload-url [success-uri]
  (.createUploadUrl (get-blobstore-service) success-uri))

(defn do-serve-blob 
  ([^GcsFilename gcs-filename http-response]
   (do-serve-blob ((.getBucketName gcs-filename) (.getObjectName gcs-filename) http-response)))
  ([bucket-name object-name http-response]
   (let [blob-key (.createGsBlobKey (get-blobstore-service) (str "/gs/" bucket-name "/" object-name))]
     (.serve (get-blobstore-service) blob-key http-response))))

(defn get-named-file-contents
  [form-element-name request]
  (.get (.getUploadedBlobs (get-blobstore-service) request) form-element-name))
