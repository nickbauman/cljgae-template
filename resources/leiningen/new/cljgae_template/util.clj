(ns {{name}}.util
    (:require [clojure.tools.logging :as log]))


(defmacro try-with-default [default & forms]
  "Tries to do 'forms'. If forms throws an exception, does default."
  `(try 
     (do  ~@forms)
     (catch Exception ~'ex
       (do 
         (log/warn (str "Failed with " (type ~'ex) ": " (.getMessage ~'ex) ". Defaulting to " ~default))
         ~default) )))

(defn name? [maybe-kw]
  "Gets the name of a keyword, if its a keyword, otherwise returns itself."
  (if (= clojure.lang.Keyword (type maybe-kw))
    (name maybe-kw)
    maybe-kw))

(defn in [scalar sequence]
  "Returns true if scalar value is found in sequence, otherwise returns nil"
  (some #(= scalar %) sequence))

(defn index-seq 
  [coll]
  "Returns a sequence of pairs of coll values preceeded by their zero-based index
  Example: [11 43 21] becomes ((0 11) (1 43) (2 21))"
  (partition 2 (interleave (range (count coll)) coll)))

