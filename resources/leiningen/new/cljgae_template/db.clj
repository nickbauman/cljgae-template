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
              Query$SortDirection
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

(def sort-order-map {:desc Query$SortDirection/DESCENDING
                     :asc Query$SortDirection/ASCENDING
                     nil Query$SortDirection/ASCENDING})

(defn filter-map
  [keyw jfilter-predicates]
  (if (= :or keyw)
    (Query$CompositeFilterOperator/or jfilter-predicates)
    (Query$CompositeFilterOperator/and jfilter-predicates)))

(defn get-option
  [options option?]
  (println (str "OPTIONS: " options " OPTION?: " option?))
  (let [indexed-pairs (u/index-seq options)]
    (if-let [[[index _]] (seq (filter #(= option? (second %)) indexed-pairs))]
      (do
        (println (str "INDEX: " index))
        (get (vec options) (inc index))))))

(defn apply-sort
  [query options]
  (if-let [order-prop (get-option options :order-by)]
    (.addSort query (name order-prop) (get sort-order-map (get-option options order-prop)))
    query))

(defn keys-only?
  [options]
  (get-option options :keys-only))

(defn make-property-filter
  [pred-coll]
   (let [[property operator-fn query-value] pred-coll
         filter-operator (operator-map operator-fn)]
     (if filter-operator
       (Query$FilterPredicate. (name property) filter-operator query-value)
       (throw (RuntimeException. (str "operator " operator-fn " not found in operator-map " (keys operator-map)))))))

(declare compose-query-filter)

(defn compose-predicates
  [preds-coll]
  (loop [jfilter-preds []
         preds preds-coll]
    (if (seq preds)
      (if (u/in (ffirst preds) [:or :and])
        (recur (conj jfilter-preds (compose-query-filter (first preds)))
               (rest preds))
        (recur (conj jfilter-preds (make-property-filter (first preds)))
               (rest preds)))
      jfilter-preds)))

(defn apply-filters-to-query
  [predicates options ent-name filters]
  (let [pred-filters (if (nil? filters) (make-property-filter predicates) filters)
        query (.setFilter (Query. ent-name) pred-filters)
        query (if (keys-only? options) (.setKeysOnly query) query)]
    (apply-sort query options)))

(defn compose-query-filter
  [preds-vec]
  (if (u/in (first preds-vec) [:and :or])
    (let [condition (first preds-vec)
          jfilter-predicates (compose-predicates (rest preds-vec))]
      (filter-map condition jfilter-predicates))))

(defn make-query
  [predicates options ent-name]
  (if (seq predicates)
    (if-let [jcomp-filter (compose-query-filter predicates)]
      (apply-filters-to-query predicates options ent-name jcomp-filter) 
      (apply-filters-to-query predicates options ent-name nil))
    ; caution: returns all!
    (let [q (Query. ent-name)
          q (if (keys-only? options) (.setKeysOnly q) q)]
      (apply-sort q options))))

(defn query-entity
  [predicates options ent-sym]
  (let [ds (DatastoreServiceFactory/getDatastoreService)
        ent-name (name ent-sym)
        query (make-query predicates options ent-name)
        prepared-query (.prepare ds query)
        result-jlist (.asList prepared-query (FetchOptions$Builder/withDefaults))
        result-entities (map #(gae-entity->map %) result-jlist)]
    (if (seq result-entities)
      result-entities)))

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

       (defn ~(symbol (str 'query- name)) [predicates# & options#]
         (if-let [results# (query-entity predicates# (first options#) '~sym)]
           (if (keys-only? (first options#))
             (map :key results#)
             (map #(merge ~empty-ent %) results#))))
       
       (defn ~(symbol (str 'delete- name)) [key#]
         (delete-entity '~sym key#)))))

