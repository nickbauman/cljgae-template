(ns {{name}}.test.util
    (:require [clojure.test :refer :all]
            [{{name}}.util :refer :all]))


(deftest test-try-with-default

    (is (= (try-with-default :default (name :foo))
           "foo"))

    (is (= (try-with-default :default (throw (Exception. "Message: Exception thrown")))
           :default)))
