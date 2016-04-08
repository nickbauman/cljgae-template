(ns {{name}}.test.handler
  (:import [com.google.appengine.tools.development.testing LocalTaskQueueTestConfig]
           [com.google.appengine.api.taskqueue.dev LocalTaskQueue])
  (:require [{{name}}.env :as env]
            [{{name}}.gcs :as gcs]
            [{{name}}.test.fixtures :as fixtures]
            [{{name}}.util :refer [try-with-default gen-file-upload-url]]
            [clj-time.core :as t])
  (:use clojure.test
    ring.mock.request
    {{name}}.handler))

(use-fixtures :once fixtures/setup-local-service-test-helper)

(defn- has-content? [response query]
  (>= (.indexOf (:body response) query) 0))

(defn- get-default-queue [task-queue]
  (get (.getQueueStateInfo task-queue) "default"))

(defn- get-tasks [queue-state-info]
  (->> queue-state-info
       .getTaskInfo
       (map bean)
       (sort-by :task-name)))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (has-content? response "{{name}} App Engine App"))))
    
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404))))
  
  (testing "upload file"
    (let [expected-filename "example.txt"
          gend-upload-url (gen-file-upload-url (str "/save/" expected-filename))
          _ (println gend-upload-url)
          response (app (request :post gend-upload-url {:file (clojure.java.io/file "file_example.jpg")}))
          input-channel (gcs/open-input-channel env/gcs-bucket-name expected-filename)]
      (is (= (:status response) 201))
      ; not working yet
      (is (= "fail" (try-with-default "Not Found!" (slurp (gcs/to-input-stream input-channel))))))))
