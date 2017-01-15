(ns {{name}}.appidentity
  (:require [clj-time.coerce :as c]
            [{{name}}.util :refer :all])
  (:import [com.google.appengine.api.utils SystemProperty]
           [com.google.appengine.api.appidentity AppIdentityServiceFactory AppIdentityService]))

(defn- property-or [property alternative]
  (or (.get property) alternative))

(defn- application-version []
  (property-or SystemProperty/applicationVersion "Development:development.0"))

(defn app-version []
    (->
     (application-version)
     (.split ":|\\.")
     (nth 1 "development")))

(defn environment []
  (property-or SystemProperty/environment "Development"))

(defn application-id []
  (property-or SystemProperty/applicationId "localhost"))

(defn sdk-version []
  (property-or SystemProperty/version "Google App Engine/1.x.x"))

(defn last-deployed-datetime [] 
  (let [version-str (application-version)
        number (Long/parseLong (last (seq (.split version-str "\\."))))
        epoch (-> number 
                (/ (Math/pow 2 28))
                (* 1000))]
        (c/from-long (long epoch))))

(defn default-gcs-bucket []
  (try-with-default "default-bucket" 
    (-> 
      (AppIdentityServiceFactory/getAppIdentityService)
      .getDefaultGcsBucketName)))
