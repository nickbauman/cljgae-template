(ns {{name}}.test.handler
  (:import [com.google.appengine.tools.development.testing LocalTaskQueueTestConfig]
           [com.google.appengine.api.taskqueue.dev LocalTaskQueue])
  (:require [{{name}}.env :as env]
            [{{name}}.gcs :as gcs]
            [{{name}}.test.fixtures :as fixtures]
            [{{name}}.util :refer [try-with-default]]
            [clj-time.core :as t])
  (:use clojure.test
    ring.mock.request
    {{name}}.handler))

(defn- has-content? [response query]
  (>= (.indexOf (:body response) query) 0))

(defn- get-default-queue [task-queue]
  (get (.getQueueStateInfo task-queue) "default"))

(defn- get-tasks [queue-state-info]
  (->> queue-state-info
       .getTaskInfo
       (map bean)
       (sort-by :task-name)))

(def content-in-file "this is the file contents, which is a text file")

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
          response (app (request :post (str "/save/" expected-filename) {:file content-in-file}))
          input-channel (gcs/open-input-channel env/gcs-bucket-name expected-filename)]
      (is (= (:status response) 201))

      (is (= content-in-file (try-with-default "Not Found!" (slurp (gcs/to-input-stream input-channel))))))))
