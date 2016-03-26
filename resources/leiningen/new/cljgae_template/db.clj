(ns {{name}}.db
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.logging :as log])
  (:import [com.google.appengine.api.datastore DatastoreService 
                                               DatastoreServiceFactory 
                                               Entity
                                               EntityNotFoundException
                                               KeyFactory]))

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
    (log/debugf "Querying %s:%s: found %s" entity-kind entity-key (pr-str result))
    result))

(defn delete-entity [entity-kind entity-key & more-keys]
  (let [datastore (DatastoreServiceFactory/getDatastoreService)]
    (.delete datastore (map #(make-key entity-kind %) (conj more-keys entity-key)))))

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

    (defn ~(symbol (str 'delete- name)) [key#]
      (delete-entity '~sym key#)))))
