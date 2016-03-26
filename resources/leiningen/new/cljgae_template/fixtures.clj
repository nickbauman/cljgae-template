(ns {{name}}.test.fixtures
(:import [com.google.appengine.tools.development.testing 
          LocalServiceTestConfig 
          LocalServiceTestHelper 
          LocalFileServiceTestConfig 
          LocalDatastoreServiceTestConfig
          LocalTaskQueueTestConfig])
(:require [clojure.java.io :as io])
(:use clojure.test
      ring.mock.request
      {{name}}.handler
      {{name}}.gcs))

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

(defn- create-local-test-helper []
  (LocalServiceTestHelper. (into-array LocalServiceTestConfig [(LocalFileServiceTestConfig.) 
                                                               (doto 
                                                                   (LocalDatastoreServiceTestConfig.) 
                                                                 (.setNoStorage true))
                                                               (queue-config)])))

(defn setup-local-service-test-helper [f] 
  (let [helper (create-local-test-helper)]
    (try 
      (.setUp helper)
      (f)
      (finally 
        (.tearDown helper)))))

