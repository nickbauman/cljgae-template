(ns {{name}}.test.appengine
    (:require [clojure.test :refer :all]
              [clj-time.coerce :as c]
              [{{name}}.appengine :refer :all]))

(deftest app-version-test
    (is (= "development" (app-version))))

(deftest last-deployed-datetime-test
    (is (= 0 (c/to-long (last-deployed-datetime)))))
