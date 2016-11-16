(ns {{name}}.test.db
    (:require [clojure.test :refer :all]
              [{{name}}.test.fixtures :as fixtures]
              [{{name}}.db :as db :refer [defentity save! delete! !=]]
              [clj-time.core :as t]))

(use-fixtures :once fixtures/setup-local-service-test-helper)

(defentity BasicEntity [content saved-time])

(defentity AnotherEntity [content saved-time int-value])

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
  
(deftest test-query-language
  (testing "query entity with predicates"
    (let [entity (save! (create-AnotherEntity "Some content woo" (t/date-time 1980 3 5) 6))
          entity2 (save! (create-AnotherEntity "Other content" (t/date-time 1984 10 12) 17))]
                                        ; query all
      (is (= (list entity entity2) (query-AnotherEntity [])))
                                        ; equality
      (is (= (list entity) (query-AnotherEntity [:content = "Some content woo"])))
      (is (nil? (query-AnotherEntity [:content = "Blearg not found"])))
                                        ; not equal
      (is (= (list entity2 entity) (query-AnotherEntity [:content != "Not found"])))
      (is (= (list entity) (query-AnotherEntity [:content != "Other content"])))
                                        ; greater-than and less-than
      (is (= (list entity) (query-AnotherEntity [:int-value < 7])))
      (is (nil? (query-AnotherEntity [:int-value < 5])))
                                        ; time: before and after
      (is (= (list entity entity2) (query-AnotherEntity [:saved-time > (.toDate (t/date-time 1979 3 5))])))
      (is (nil? (query-AnotherEntity [:saved-time < (.toDate (t/date-time 1979 3 5))])))
                                        ; "and" compound queries
      (is (= (list entity) (query-AnotherEntity [:and [:content = "Some content woo"] [:int-value > 5]])))
      (is (= (list entity entity2) (query-AnotherEntity [:and [:int-value > 5] [:int-value <= 17]])))
                                        ; "or" compound queries
      (is (= (list entity) (query-AnotherEntity [:or [:content = "Some content woo"] [:int-value < 5]])))
      (is (= (list entity entity2) (query-AnotherEntity [:or [:content = "Some content woo"] [:int-value > 5]])))
                                        ; compound queries with nested compound predicates
      (is (= (list entity entity2) (query-AnotherEntity [:or [:content = "Other content"] [:and [:saved-time < (.toDate (t/date-time 1983 3 5))] [:int-value = 6]]]))))))


