(ns {{name}}.handler
  (:require [compojure.core :refer [defroutes routes POST GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.multipart-params :as multipart-params]
            [ring.middleware.multipart-params.byte-array :as upload-store]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [{{name}}.env :as env]
            [{{name}}.gcs :as gcs]
            ;[{{name}}.push-queue :as pq]
            [{{name}}.db :as db]
            [{{name}}.env :as env]
            [{{name}}.model :as m]
            [{{name}}.view :refer [home file-upload-form]]))

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

(defroutes app-routes
  (GET "/" [:as r] (home r))
  (GET "/save" [:as r] (file-upload-form r))
  (POST "/save" {params :params} (do-save params))
  (route/resources "/")
  (route/not-found "Page not found"))

(defn wrap-middleware [routes]
  (let [modified-defaults 
        (-> site-defaults
            (assoc :params {:multipart {:store (upload-store/byte-array-store)}})
            (assoc :security {:anti-forgery false}))]
    (wrap-defaults routes modified-defaults)))

(def app
  (-> (routes app-routes)
      (wrap-middleware)))
