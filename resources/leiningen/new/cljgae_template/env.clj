(ns {{name}}.env
  (:require [{{name}}.appidentity :as a]))

(def environment (let [app-eng-env (a/environment)
                       app-name (a/application-id)]
                    (condp = [app-eng-env app-name]
                      ["Production" "{{name}}"] :production
                      ["Production" "{{name}}-qa"] :qa
                      ["Production" "{{name}}-int"] :integration
                      :development)))

(defmacro defenv [name & conditions]
  `(def ~name 
    (condp = environment
      ~@conditions)))

(defenv gcs-bucket-name 
  :production "{{name}}"
  :qa (str "{{name}}-qa")
  :integration "{{name}}-int"
  :development (a/default-gcs-bucket))
