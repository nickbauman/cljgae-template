(ns {{name}}.handler
  (:require [compojure.core :refer [defroutes routes POST GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.multipart-params :as multipart-params]
            [ring.middleware.multipart-params.byte-array :as upload-store]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [gaeclj.env :as env]
            [gaeclj.gcs :as gcs]
            [gaeclj.ds :as db]
            [gaeclj.env :as env]
            [gaeclj.push-queue :as pq]
            [{{name}}.util :as u]
            [{{name}}.model :as m]
            [{{name}}.view :refer [home file-upload-form]])
  (:import [java.io InputStreamReader]))

(defn init []
  (log/info "{{name}} is starting on" env/environment))

(defn destroy []
  (log/info "{{name}} is shutting down"))

(defn do-save 
  "Saves a given file to the app's GCS bucket.
  - params: the request as defined by the multipart middleware with a bytearray store"
  [params]
  (log/warnf "params %1$s" params)
  (let [the-file (get params "thefile")
        uploaded-binary (get the-file :bytes)
        content-type (get the-file :content-type)
        filename (get the-file :filename)]
    (gcs/with-gcs-output-stream gcs-writer env/gcs-bucket-name filename
      (.write gcs-writer uploaded-binary)
      (db/save! (m/create-FileUpload filename (t/now)))
      (log/infof "Succesfully wrote %1$s to /%2$s/%1$s" filename env/gcs-bucket-name)
      (response/redirect "/save"))))

(defn process-json-bg
  "Takes a largeish JSON file and processes it in the background in parts via a push queue
  - request: the ring request"
  [request]
  (let [data (json/read (InputStreamReader. (:body request)))]
    (loop [[d & dd] data]
      (pq/add-to-queue 
       (pq/get-queue "default")
       (apply pq/task-options "/save-json" 1000 (mapcat #(identity %) d)))
      (if (not (seq dd))
        {:status 202}
        (recur dd)))))

; TODO actually process the data
(defn save-json
  [request]
  {:status 200})

(defroutes app-routes
  (GET "/" [:as r] (home r))
  (GET "/save" [:as r] (file-upload-form r))
  (POST "/save" {params :params} (do-save params))
  (POST "/process-json-bg" [:as r] (process-json-bg r))
  (GET "/save-json" [:as r] (save-json r))
  (route/resources "/")
  (route/not-found "Page not found"))

(defn wrap-middleware [routes]
  (let [modified-defaults 
        (-> site-defaults
            (assoc :params {:multipart {:store (upload-store/byte-array-store)}}) ; TODO use GCS directly instead of the byte array store
            (assoc :security {:anti-forgery false}))] ; TODO AF might be desired, get it working with test harness
    (wrap-defaults routes modified-defaults)))

(def app
  (-> (routes app-routes)
      (wrap-middleware)))
