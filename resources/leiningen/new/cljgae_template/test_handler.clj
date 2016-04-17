(ns {{name}}.test.handler
  (:import [com.google.appengine.tools.development.testing LocalTaskQueueTestConfig]
           [com.google.appengine.api.taskqueue.dev LocalTaskQueue])
  (:require [{{name}}.env :as env]
            [{{name}}.gcs :as gcs]
            [{{name}}.test.fixtures :as fixtures]
            [{{name}}.util :refer [try-with-default]]
            [clojure.java.io :as io]
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
    (let [expected-filename "foobarbaz/test/file_example.jpg"
          expected-file-contents (slurp (io/file (io/resource expected-filename)))
          gend-upload-url (gcs/gen-file-upload-url (str "/save"))
          req (->
               (request :post "/save" {:multipart-params {:file expected-file-contents :filename expected-filename}})
               (update :headers assoc
                       "content-type" "multipart/form-data; boundary=ABCD")
               (assoc :content-type "multipart/form-data; boundary=ABCD")
               (assoc :enctype "multipart/form-data"))
          response (app req)
          input-channel (gcs/open-input-channel env/gcs-bucket-name expected-filename)
          actual-filecontents (try-with-default "Not Found!" (slurp (gcs/to-input-stream input-channel)))]
      
      (is (= (:status response) 201))
      (is (= (type expected-file-contents) (type actual-filecontents)))
      (is (= expected-file-contents actual-filecontents)))))
