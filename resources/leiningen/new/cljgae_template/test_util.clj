(ns {{name}}.test.util
    (:require [clojure.test :refer :all]
            [{{name}}.util :refer :all]))


(deftest test-try-with-default

    (is (= (try-with-default :default (name :foo))
           "foo"))

    (is (= (try-with-default :default (throw (Exception. "Message: Exception thrown")))
           :default)))

(deftest test-name?
  (is (= "g" (name? :g)))
  (is (= "g" (name? "g")))
  (is (= 1 (name? 1))))
