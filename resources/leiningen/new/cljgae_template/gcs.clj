(ns {{name}}.gcs
  (:import [com.google.appengine.tools.cloudstorage GcsService
                                                    GcsServiceFactory
                                                    GcsFilename
                                                    GcsFileOptions$Builder]
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
