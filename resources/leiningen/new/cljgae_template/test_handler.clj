(ns {{name}}.test.handler
    (:require [{{name}}.env :as env]
              [{{name}}.gcs :as gcs]
              [{{name}}.handler :as handler]
              [{{name}}.test.helpers :as helper]
              [{{name}}.test.fixtures :as fixtures]
              [{{name}}.util :refer [try-with-default]]
              [ring.util.codec :refer [form-decode]]
              [clojure.data.json :as json]
              [clojure.java.io :as io]
              [clj-time.core :as t])
    (:use clojure.test
          ring.mock.request))

(use-fixtures :once fixtures/setup-local-service-test-helper)

(defn- has-content? [response query]
  (>= (.indexOf (:body response) query) 0))

(deftest test-app
  (testing "main route"
    (let [response (handler/app (request :get "/"))]
      (is (= (:status response) 200))
      (is (has-content? response "{{name}} App Engine App"))))
    
  (testing "not-found route"
    (let [response (handler/app (request :get "/invalid"))]
      (is (= (:status response) 404))))

  (testing "upload file and store in google cloud storage"
    (let [expected-filename "test/foobarbaz/test/file_example.jpg"
          temp-file (helper/create-temp-file expected-filename)
          file-data (helper/get-file-contents (.getAbsolutePath temp-file))
          response (handler/do-save {"thefile" {:bytes file-data :content-type "image/jpeg" :filename expected-filename}})
          input-channel (gcs/open-input-channel env/gcs-bucket-name expected-filename)
          actual-filecontents (try-with-default "Not Found!" (slurp (gcs/to-input-stream input-channel)))]
      (is (= (:status response) 302))
      (is (= (String. file-data) actual-filecontents))))

  (testing "post data for later processing via appengine task queue"
    (let [json-to-process (helper/get-file-contents "test/foobarbaz/test/events.json")
          all-json-processed (json/read-str (String. json-to-process "UTF-8"))
          req (-> (request :post "/process-json-bg")
                  (body json-to-process)
                  (content-type "application/json"))
          response (handler/app req)]
      
      (is (= (:status response) 202))
      
      (let [queue-state-info (helper/get-default-queue (helper/get-local-queue-infra))
            tasks (helper/get-tasks queue-state-info)]
        ; break it into parts
        (is (= 24 (.getCountTasks queue-state-info)))
        (is (= 24 (count tasks)))
        
        (let [task-info (first tasks)
              decoded-task-body (form-decode (:body task-info))
              expected-task (first (filter #(= (str (get % "event_ts")) (get decoded-task-body "event_ts")) all-json-processed))]
          (is (= "/save-json" (:url task-info)))
          (is (= (.size expected-task)
                 (.size decoded-task-body)))
          (is (= (.keySet expected-task)
                 (.keySet decoded-task-body)))))))
  
  (helper/flush-test-queue "default"))
