(ns {{name}}.view
  (:require [hiccup.page :as page]
    [{{name}}.push-queue :as pg]
    [{{name}}.appengine :refer [sdk-version app-version
                                             application-id environment
                                             last-deployed-datetime]]
    [{{name}}.env :as env]
    [clj-time.format :as f]
    [clj-time.core :as t]))

(defn- module-hostname [request]
  (or (pg/get-module-hostname)
    (get-in request [:headers "host"])))

(defn- tr [type & cols]
  (cons [:tr] (map #(vector type %) cols)))

(def ^:private formatter (f/with-zone 
                           (f/formatters :rfc822)
                           (t/time-zone-for-id "America/Chicago")))

(defn home [request] 
  (page/html5 
    [:head
     [:title "{{name}} App Engine App"]
     (page/include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")]
    [:body
     [:div {:class "container"}
      [:h1 "{{name}}"]
      [:table {:class "table table-bordered table-striped table-condensed"}
       (tr :th "Property" "Value")
       (tr :td "Application Id" (application-id))
       (tr :td "Version" (app-version))
       (tr :td "Module Hostname" (module-hostname request))
       (tr :td "GCS Bucket" env/gcs-bucket-name)
       (tr :td "Last Deployed At" (f/unparse formatter (last-deployed-datetime)))
       (tr :td "Environment" (environment))
       (tr :td "SDK Version" (sdk-version))]]
     (page/include-js "//code.jquery.com/jquery-2.1.1.min.js")
     (page/include-js "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js")]))
