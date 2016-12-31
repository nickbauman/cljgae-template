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
              Key
              Query
              Query$SortDirection
              Query$CompositeFilter
              Query$CompositeFilterOperator
              Query$FilterPredicate
              Query$FilterOperator]))

(defprotocol NdbEntity
  (save! [this] [this parent-key] "Saves the entity. Saves the entity with its parent key.")
  (delete! [this] "Deletes the entity")
  (gae-key [this] "Produces Java SDK Appengine key from Clojure NdbEntity information"))

(defprotocol ToNdbValue
  (->prop [v] "Converts the value to a ndb appropriate value"))

(defprotocol FromNdbValue
  (<-prop [v] "Converts the value from a ndb appropriate value"))

(extend-protocol ToNdbValue
  Key
  (->prop [k] k)

  String
  (->prop [s] s)

  Number
  (->prop [n] n)

  java.util.Date
  (->prop [d] d)

  org.joda.time.DateTime
  (->prop [d] (c/to-date d)))

(extend-protocol FromNdbValue
  Key
  (<-prop [k] k)

  String
  (<-prop [s] s)
  
  Number
  (<-prop [n] n)

  java.util.Date
  (<-prop [d] (c/from-date d)))

(defn make-key 
  ([kind value]
   (KeyFactory/createKey (name kind) value))
  ([kind parent value]
   (KeyFactory/createKey parent (name kind) value)))

(defn check-key
  [parent-key entity-kind]
  (if parent-key
    (if (isa? (type parent-key) Key)
      parent-key
      (throw (RuntimeException. (str "parent key " parent-key " not an instance of " Key))))))

(defn save-entity
  ([entity-type entity]
   (save-entity entity-type entity nil))
  ([entity-type entity parent-key]
    (log/debugf "Saving %s: %s" entity-type (pr-str entity))
    (let [datastore (DatastoreServiceFactory/getDatastoreService)
          gae-parent-key (check-key parent-key entity-type)
          gae-ent (if (:key entity)
                    (if gae-parent-key
                      (Entity. (name entity-type) (:key entity) gae-parent-key)
                      (Entity. (name entity-type) (:key entity)))
                    (if gae-parent-key
                      (Entity. (name entity-type) gae-parent-key)
                      (Entity. (name entity-type))))]
      (doseq [field (keys entity)]
        (.setProperty gae-ent (name field) (->prop (field entity))))
      (try 
        (.put datastore gae-ent)
        (catch Exception e
          (log/errorf "Unable to save %s" (pr-str entity)) e))
      (if (:key entity)
        entity
        (assoc entity :key (.. gae-ent getKey getId))))))

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

(defn as-gae-key
  [entity-kind entity-key]
  (make-key entity-kind entity-key))

(defn delete-entity [entity-kind entity-key & more-keys]
  (let [datastore (DatastoreServiceFactory/getDatastoreService)]
    (.delete datastore (map #(make-key entity-kind %) (conj more-keys entity-key)))))

                                        ; Start DS Query support ;;;

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
  (let [indexed-pairs (map-indexed vector options)]
    (if-let [[[index _]] (seq (filter #(= option? (second %)) indexed-pairs))]   
      (get (vec options) (inc index)))))

(defn add-sorts
  [options query]
  (if-let [ordering (if (and (seq options) (not (= -1 (.indexOf options :order-by)))) (rest (subvec options (.indexOf options :order-by))))]
    (loop [[[order-prop direction] & more] (partition 2 ordering)]
      (if (and order-prop direction (not (= order-prop :keys-only)))
        (do (.addSort query (name order-prop) (get sort-order-map direction)) (recur more))
        query))
    query))

(defn set-keys-only
  [options query]
  (if (get-option options :keys-only)
    (.setKeysOnly query)
    query))

(defn set-ancestor-key
  [options entity-kind query]
  (if-let [parent-key (check-key (get-option options :ancestor-key) entity-kind)]
    (.setAncestor query parent-key)
    query))

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
      (let [filter-fn (if (u/in (ffirst preds) [:or :and]) 
                        compose-query-filter 
                        make-property-filter)]
        (recur (conj jfilter-preds (filter-fn (first preds)))
               (rest preds)))
      jfilter-preds)))

(defn compose-query-filter
  [preds-vec]
  (if (u/in (first preds-vec) [:and :or])
    (let [condition (first preds-vec)
          jfilter-predicates (compose-predicates (rest preds-vec))]
      (filter-map condition jfilter-predicates))))

(defn qbuild
  [predicates options ent-kind filters]
  (->> (if (nil? filters) (make-property-filter predicates) filters)
       (.setFilter (Query. (name ent-kind)))
       (set-ancestor-key options ent-kind)
       (set-keys-only options)
       (add-sorts options)))

(defn make-query
  [predicates options ent-kind]
  (if (seq predicates)
    (qbuild predicates options ent-kind (compose-query-filter predicates)) 
    (->> (Query. (name ent-kind))
         (set-ancestor-key options ent-kind)
         (set-keys-only options)
         (add-sorts options))))

(defn lazify-qiterable
  ([pq-iterable]
   (lazify-qiterable pq-iterable (.iterator pq-iterable)))
  ([pq-iterable i]
   (lazy-seq 
    (when (.hasNext i)
      (cons (gae-entity->map (.next i)) (lazify-qiterable pq-iterable i))))))

(defn query-entity
  [predicates options ent-sym]
  (->> ent-sym 
       (make-query predicates options)
       (.prepare (DatastoreServiceFactory/getDatastoreService))
       .asIterable
       lazify-qiterable
       seq))

(defmacro with-transaction [& body]
  `(let [tx# (.beginTransaction (DatastoreServiceFactory/getDatastoreService))]
     (try
       (let [body-result# (do ~@body)]
         (.commit tx#)
         body-result#)
       (catch Throwable err#
         (do (.rollback tx#)
             (throw err#))))))

                                        ; End DS Query support ;;;
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
         (save! [this# parent-key#] (save-entity '~sym this# parent-key#))
         (delete! [this#] (delete-entity '~sym (:key this#)))
         (gae-key [this#] (as-gae-key '~sym (:key this#))))

       (def ~empty-ent
         ~(conj (map (constantly nil) entity-fields) creator))

       (defn ~(symbol (str 'create- name)) ~entity-fields
         ~(conj (seq entity-fields) creator))

       (defn ~(symbol (str 'get- name)) [key#]
         (if-let [result# (get-entity '~sym key#)]
           (merge ~empty-ent result#)))

       (defn ~(symbol (str 'query- name)) [predicates# & options#]
         (if-let [results# (query-entity predicates# (first options#) '~sym)]
           (if (get-option (first options#) :keys-only)
             (map :key results#)
             (map #(merge ~empty-ent %) results#))))
       
       (defn ~(symbol (str 'delete- name)) [key#]
         (delete-entity '~sym key#)))))
