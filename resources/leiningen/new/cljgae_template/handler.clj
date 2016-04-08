(ns {{name}}.handler
  (:require [compojure.core :refer [defroutes routes POST GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.session :as session]
            [ring.middleware.multipart-params :as multipart-params]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [{{name}}.env :as env]
            [{{name}}.gcs :as gcs]
            ;[{{name}}.push-queue :as pq]
            [{{name}}.db :as db]
            [{{name}}.env :as env]
            [{{name}}.model :as m]
            [{{name}}.util :as u]
            [{{name}}.view :refer [home]]))

(defn init []
  (log/info "{{name}} is starting on" env/environment))

(defn destroy []
  (log/info "{{name}} is shutting down"))

(def IN_TWO_MIN 1200000)

(defn do-save 
  "Saves a given file to the app's GCS bucket.
  - filename: the remote file to import
  - request: the netire Ring request"
  [filename request]
  (log/debugf "Saving file %1$s" filename)
  (let [uploaded-binary (u/get-named-file-contents "file" request)]
    (gcs/with-gcs-output-stream gcs-writer env/gcs-bucket-name filename
      (.write gcs-writer uploaded-binary)
      (db/save! (m/create-FileUpload filename (t/now)))
      (log/infof "Succesfully wrote %1$s to /%2$s/%1$s" filename env/gcs-bucket-name))))

(defroutes app-routes
  (GET "/" [:as r] (home r))
  (POST "/save/:filename" [filename :as r] (do-save filename r))
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/api)
      (session/wrap-session)
      (multipart-params/wrap-multipart-params)))

