(ns {{name}}.gcs
  (:import [com.google.appengine.tools.cloudstorage GcsService
                                                    GcsServiceFactory
                                                    GcsFilename
                                                    GcsFileOptions$Builder]
           [com.google.appengine.api.blobstore BlobstoreService BlobstoreServiceFactory]
           [java.nio.channels Channels]))

(def ^:private file-options 
  (-> 
    (GcsFileOptions$Builder.)
    (.mimeType "text/plain; charset=utf-8")
    (.build) ))

(defn- gcs-filename [bucket-name filename]
  (GcsFilename. bucket-name filename))

(defn open-output-channel [^String bucket-name ^String filename]
  (let [service (GcsServiceFactory/createGcsService)]
    (.createOrReplace service 
      (gcs-filename bucket-name filename)
      file-options)))

(defn to-output-stream [gcs-output-channel]
  (Channels/newOutputStream gcs-output-channel))

(defn open-input-channel [^String bucket-name ^String filename]
  (let [service (GcsServiceFactory/createGcsService)]
    (.openReadChannel service 
      (gcs-filename bucket-name filename) 0)))

(defn to-input-stream [gcs-input-channel]
  (Channels/newInputStream gcs-input-channel))

(defmacro with-gcs-output-stream [ os bucket-name filename & body]
  `(let [~'gcs-writer (gcs/open-output-channel ~bucket-name ~filename)
         ~os (gcs/to-output-stream ~'gcs-writer)]
    (try 
      (do ~@body)
     (finally
        (.close ~'gcs-writer)))))

(defmacro with-gcs-input-stream [is bucket-name filename & body]
  `(let [~'gcs-reader (gcs/open-input-channel ~bucket-name ~filename)
         ~is (gcs/to-input-stream ~'gcs-reader)]
    (try 
      (do ~@body)
     (finally
        (.close ~'gcs-reader)))))

; Blobstore support

(defn get-blobstore-service []
  "Get the BlobStoreService, factory construct if necessary"
  (BlobstoreServiceFactory/getBlobstoreService))

(defn gen-file-upload-url [success-uri]
  "Generate a blobstore upload URL"
  (.createUploadUrl (get-blobstore-service) success-uri))

(defn do-serve-blob
  ([^GcsFilename gcs-filename http-response]
   "Serve a file from GCS using the blobstore API from a GcsFilename"
   (do-serve-blob ((.getBucketName gcs-filename) (.getObjectName gcs-filename) http-response)))
  ([bucket-name object-name http-response]
   "Serve a file from GCS using the blobstore API from a bucket name and an object name/path-like string"
   (let [blob-key (.createGsBlobKey (get-blobstore-service) (str "/gs/" bucket-name "/" object-name))]
     (.serve (get-blobstore-service) blob-key http-response))))

(defn get-named-file-contents
  [form-element-name request]
  "Retrieve a blob uploaded named after its form element name from a request"
  (.get (.getUploadedBlobs (get-blobstore-service) request) form-element-name))
