(ns {{name}}.test.db
  (:require [clojure.test :refer :all]
            [{{name}}.test.fixtures :as fixtures]
            [{{name}}.db :as db :refer [defentity save! delete!]]
            [clj-time.core :as t]))

(use-fixtures :once fixtures/setup-local-service-test-helper)

(defentity BasicEntity [content saved-time])

(defentity CustomKeyEntity [key content saved-time])

(deftest test-basic-entities

  (testing "Save basic entity"
    (let [ent (create-BasicEntity "Some content" (t/date-time 1984 6 8))
          saved-ent (save! ent)]
      (is (:key saved-ent))
      (is (= saved-ent (get-BasicEntity (:key saved-ent))))))

  (testing "Delete basic entity"
    (let [ent (create-BasicEntity "Some content" (t/date-time 1984 6 8))
          saved-ent (save! ent)
          ent-key (:key saved-ent)]

      (is (get-BasicEntity ent-key))

      (delete! saved-ent)

      (is (nil? (get-BasicEntity ent-key))))))

(deftest test-custom-key-entities
  (testing "Save custom key entity"
    (let [ent (create-CustomKeyEntity "my-key" "some content" (t/date-time 1999 12 31))
          saved-ent (save! ent)]
      (is (= "my-key" (:key saved-ent)))
      (is (= ent (get-CustomKeyEntity "my-key")))))

  (testing "Delete custom key entity"
    (let [ent (create-CustomKeyEntity "my-key" "some content" (t/date-time 1999 12 31))
          _ (save! ent)]

      (delete! ent)
      (is (nil? (get-CustomKeyEntity "my-key"))))))

(deftest test-basic-gae-functions 
  (testing "save-entity and get-entity"
    (let [saved-time (t/now)
          saved-ent (db/save-entity 'BasicEntity {:content "Something Saved" :saved-time saved-time})
          fetched-ent (db/get-entity 'BasicEntity (saved-ent :key))]
      (is (:key saved-ent))

      (are [x y] (= x y)
        "Something Saved" (:content fetched-ent)
        saved-time (:saved-time fetched-ent)))))
