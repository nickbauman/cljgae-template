(ns {{name}}.db
    (:require [clj-time.core :as t]
              [clj-time.coerce :as c]
              [clojure.reflect :as r]
              [clojure.tools.logging :as log]
              [{{name}}.util :as u])
    (:import [com.google.appengine.api.datastore 
              DatastoreServiceFactory
              DatastoreService
              Entity
              EntityNotFoundException
              FetchOptions$Builder
              KeyFactory
              Query
              Query$CompositeFilter
              Query$CompositeFilterOperator
              Query$FilterPredicate
              Query$FilterOperator]))

(defprotocol NdbEntity
  (save! [this] "Saves the entity")
  (delete! [this] "Deletes the entity"))

(defprotocol ToNdbValue
  (->prop [v] "Converts the value to a ndb appropriate value"))

(defprotocol FromNdbValue
  (<-prop [v] "Converts the value from a ndb appropriate value"))

(extend-protocol ToNdbValue
  String
  (->prop [s] s)

  Number
  (->prop [n] n)

  java.util.Date
  (->prop [d] d)

  org.joda.time.DateTime
  (->prop [d] (c/to-date d)))

(extend-protocol FromNdbValue
  String
  (<-prop [s] s)
  
  Number
  (<-prop [n] n)

  java.util.Date
  (<-prop [d] (c/from-date d)))

(defn- make-key [kind value]
  (KeyFactory/createKey (name kind) value))

(defn save-entity [entity-type entity]
  (log/debugf "Saving %s: %s" entity-type (pr-str entity))
  (let [datastore (DatastoreServiceFactory/getDatastoreService)
        gae-ent (if (:key entity)
                  (Entity. (name entity-type) (str (:key entity)))
                  (Entity. (name entity-type)))]
    (doseq [field (keys entity)]
      (.setProperty gae-ent (name field) (->prop (field entity))))
    (try 
      (.put datastore gae-ent)
      (catch Exception e
        (log/errorf "Unable to save %s" (pr-str entity)) e))
    (if (:key entity)
      entity
      (assoc entity :key (.. gae-ent getKey getId)))))

(defn- gae-entity->map [gae-entity]
  (let [gae-key (.getKey gae-entity)
        hm (.getProperties gae-entity)
        gae-map (apply merge (map #(hash-map (keyword %) (<-prop (get hm %))) (keys hm)))]
    (assoc gae-map :key (if (.getName gae-key)
                          (.getName gae-key)
                          (.getId gae-key)))))

(defn get-entity [entity-kind entity-key]
  (let [datastore (DatastoreServiceFactory/getDatastoreService)
        result (try
                 (gae-entity->map (.get datastore (make-key entity-kind entity-key)))
                 (catch EntityNotFoundException e
                   nil))]
    (log/debugf "Getting %s:%s: found %s" entity-kind entity-key (pr-str result))
    result))

(defn delete-entity [entity-kind entity-key & more-keys]
  (let [datastore (DatastoreServiceFactory/getDatastoreService)]
    (.delete datastore (map #(make-key entity-kind %) (conj more-keys entity-key)))))

(defn !=
  "Available to complete the operator-map logic. Reverse logic of the = function"
  ([x] (not (= x)))
  ([x y] (not (clojure.lang.Util/equiv x y)))
  ([x y & more]
   (not (apply = x y more))))

(def operator-map {< Query$FilterOperator/LESS_THAN
                   > Query$FilterOperator/GREATER_THAN
                   = Query$FilterOperator/EQUAL
                   >= Query$FilterOperator/GREATER_THAN_OR_EQUAL
                   <= Query$FilterOperator/LESS_THAN_OR_EQUAL
                   != Query$FilterOperator/NOT_EQUAL})

(defn composite-predicate?
  [predicates]
  (or (= (ffirst predicates) :or) (= (ffirst predicates) :and)))

(defn keys-only?
  [preds]
  (if (composite-predicate? preds)
    (keys-only? (second preds))
    (some #(= :keys-only (first %)) preds)))

(defn make-filter-predicate
  [pred-coll]
  (if (= (count pred-coll) 3)
    (let [[property key-fn query-value] pred-coll
          filter-operator (operator-map key-fn)]
      (if filter-operator
        (Query$FilterPredicate. (name property) filter-operator query-value)
        (throw (RuntimeException. (str "operator " key-fn " not found in operator-map " (keys operator-map))))))
    (if (not (and (= (count pred-coll) 1) (= (first pred-coll) :keys-only)))
      (throw (RuntimeException. (str "cannot interpret " pred-coll))))))

(defn apply-filters-to-query
  [predicates ent-name filters]
  (if (or (= (type filters) Query$CompositeFilter) (nil? filters))
    (let [pred-filters (if (nil? filters) 
                         (make-filter-predicate (first predicates))
                         filters)]
      (if (keys-only? predicates)
        (.setFilters (.setKeysOnly (Query. ent-name)) pred-filters)
        (.setFilter (Query. ent-name) pred-filters)))))

(defn make-query
  [predicates ent-name]
  (if (seq (first predicates))
    (if (composite-predicate? predicates)
      (let [composite-condition (ffirst predicates)
            jfilter-predicates (loop [jfilter-preds []
                                      preds (rest (first predicates))]
                                 (if (seq preds)
                                   (recur (conj jfilter-preds (make-filter-predicate (first preds)))
                                          (rest preds))
                                   jfilter-preds))
            jcomp-filter (if (= composite-condition :or) 
                           (Query$CompositeFilterOperator/or jfilter-predicates)
                           (Query$CompositeFilterOperator/and jfilter-predicates))]
            (apply-filters-to-query predicates ent-name jcomp-filter)) 
        (apply-filters-to-query predicates ent-name nil))
    (Query. ent-name))) ; returns all entities!

(defn run-query
  [predicates ent-sym]
  (let [ds (DatastoreServiceFactory/getDatastoreService)
        ent-name (name ent-sym)
        query (make-query predicates ent-name)
        prepared-query (.prepare ds query)
        result-jlist (.asList prepared-query (FetchOptions$Builder/withDefaults))
        result-entities (map #(gae-entity->map %) result-jlist)]
    (if (seq result-entities)
      (do
        (log/debugf "Querying %s%s found %s" ent-name (first predicates) (pr-str result-entities))
        result-entities)
      (do
        (log/debugf "Querying %s found nil" ent-name (first predicates))
         nil))))

(defmacro defentity
  [entity-name entity-fields]
  (let [name entity-name
        sym (symbol name)
        empty-ent (symbol (str 'empty- name))
        creator (symbol (str  '-> name))]
    `(do 
       (defrecord ~name ~entity-fields
         NdbEntity
         (save! [this#] (save-entity '~sym this#))
         (delete! [this#] (delete-entity '~sym (:key this#))))

       (def ~empty-ent
         ~(conj (map (constantly nil) entity-fields) creator))

       (defn ~(symbol (str 'create- name)) ~entity-fields
         ~(conj (seq entity-fields) creator))

       (defn ~(symbol (str 'get- name)) [key#]
         (if-let [result# (get-entity '~sym key#)]
           (merge ~empty-ent result#)))

       (defn ~(symbol (str 'query- name)) [& predicates#]
         (let [results# (run-query predicates# '~sym)]
           (if results#
             (map #(merge ~empty-ent %) results#))))
       
       (defn ~(symbol (str 'delete- name)) [key#]
         (delete-entity '~sym key#)))))
