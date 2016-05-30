(ns {{name}}.test.helpers
    (:require [clojure.java.io :as io])
    (:import [com.google.appengine.tools.development.testing LocalTaskQueueTestConfig]
             [com.google.appengine.api.taskqueue.dev LocalTaskQueue]))

(defn create-temp-file 
  [file-path]
  (let [filename-start (inc (.lastIndexOf file-path "/"))
        file-extension-start (.lastIndexOf file-path ".")
        file-name (.substring file-path filename-start file-extension-start)
        file-extension (.substring file-path file-extension-start)
        temp-file (java.io.File/createTempFile file-name file-extension)]
    (io/copy (io/file file-path) temp-file)
    temp-file))

(defn get-file-contents
  [file-path]
  (with-open [r (io/input-stream file-path)]
    (loop [c (.read r)
           byte-vec []] 
      (if (not= c -1)
        (recur (.read r) 
               (conj byte-vec (int c)))
        (byte-array byte-vec)))))


(defn get-local-queue-infra []
  (LocalTaskQueueTestConfig/getLocalTaskQueue))

(defn flush-test-queue
  [queue-name]
  (.flushQueue (get-local-queue-infra) queue-name))

(defn get-default-queue [task-queue]
  (get (.getQueueStateInfo task-queue) "default"))

(defn get-tasks [queue-state-info]
  (->> queue-state-info
       .getTaskInfo
       (map bean)
       (sort-by :task-name)))
