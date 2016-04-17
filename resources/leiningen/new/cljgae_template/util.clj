(ns {{name}}.util
  (:require [clojure.tools.logging :as log]))


(defmacro try-with-default [default & forms]
  `(try 
    (do  ~@forms)
    (catch Exception ~'ex
      (do 
        (log/warn (str "Failed with " (type ~'ex) ": " (.getMessage ~'ex) ". Defaulting to " ~default))
        ~default) )))

