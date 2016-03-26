(ns {{name}}.test.push-queue
  (:require [clojure.test :refer :all]
            [{{name}}.push-queue :refer :all]))

(deftest test-task-options
  (testing "no params"
    (let [opts (bean (task-options "/some-uri" 100))]
      (is (= "/some-uri" (:url opts)))))

  (testing "has eta-millis"
    (let [opts (task-options "/test-uri" 100)]
      (is (= 100 (.getEtaMillis opts)))))

  (testing "key value string params"
    (let [opts (task-options "/test-uri" 100 :foo "bar")]
      (is (> 0 (.indexOf "params=[StringParam(foo=bar)" (str opts))))))

  (testing "key value int params"
    (let [opts (task-options "/test-uri" 100 :bar 12)]
      (is (> 0 (.indexOf "params=[StringParam(bar=12)" (str opts)))))))

(deftest test-get-module-hostname 
  (testing "Should return nil in test profile"
    (is (nil? (get-module-hostname)))))

