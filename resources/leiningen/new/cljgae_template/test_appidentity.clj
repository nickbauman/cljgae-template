(ns {{name}}.test.appidentity
    (:require [clojure.test :refer :all]
              [clj-time.coerce :as c]
              [{{name}}.test.fixtures :as fixtures]
              [{{name}}.appidentity :refer :all]))

(use-fixtures :once fixtures/setup-local-service-test-helper)

(deftest app-version-test
    (is (= "development" (app-version))))

(deftest last-deployed-datetime-test
    (is (= 0 (c/to-long (last-deployed-datetime)))))

