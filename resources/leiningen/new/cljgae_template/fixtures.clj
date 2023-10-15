(ns {{name}}.test.fixtures
  (:import [com.google.appengine.tools.development.testing
            LocalServiceTestConfig
            LocalServiceTestHelper
            LocalDatastoreServiceTestConfig
            LocalTaskQueueTestConfig
            LocalAppIdentityServiceTestConfig
            LocalBlobstoreServiceTestConfig])
  (:require [clojure.java.io :as io])
  (:use clojure.test
   ring.mock.request
   cmcoop.handler
   gaeclj.gcs))

(defn delete-recursively [fname]
 (let [func (fn [func f]
             (when (.isDirectory f)
              (doseq [f2 (.listFiles f)]
               (func func f2)))
             (io/delete-file f))]
  (func func (io/file fname))))

(defn- queue-config []
 (doto
  (LocalTaskQueueTestConfig.)
  (.setQueueXmlPath "war-resources/WEB-INF/queue.xml")
  (.setDisableAutoTaskExecution true)))

(defn- datastore-config []
 (doto
  (LocalDatastoreServiceTestConfig.)
  (.setApplyAllHighRepJobPolicy)
  (.setNoStorage true)))

(defn- blobstore-config []
 (doto
  (LocalBlobstoreServiceTestConfig.)
  (.setNoStorage true)))

(defn- app-identity-config []
 (doto
  (LocalAppIdentityServiceTestConfig.)
  (.setDefaultGcsBucketName "default-bucket")))

(defn- create-local-test-helper []
 (LocalServiceTestHelper. (into-array LocalServiceTestConfig [(app-identity-config)
                                                              (blobstore-config)
                                                              (datastore-config)
                                                              (queue-config)])))

(defn setup-local-service-test-helper [f]
 (let [helper (create-local-test-helper)]
  (try
   (.setUp helper)
   (f)
   (finally
    (.tearDown helper)))))

