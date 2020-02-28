(ns leiningen.new.cljgae-template
  (:require [clojure.java.io :refer [input-stream]]
            [leiningen.new.templates :refer [renderer name-to-path ->files slurp-resource]]
            [leiningen.core.main :as main]))

(def render (renderer "cljgae-template"))

(defn raw-copy
      "Use in case of binary files that just need to be copied verbatim.
      (... the default renderer apparently tries to decode the file :/)"
      [filename, data]
      (input-stream (clojure.string/join "/" ["resources/leiningen/new/cljgae_template" filename])))

(defn cljgae-template
  "Expands template vars over files, as needed"
  [name]
  (let [data {:name name
              :year (+ 1900 (.getYear (new java.util.Date)))
              :sanitized (name-to-path name)}]
    (main/info (str "Generating new cljgae-template project '" name "'"))
    (->files data
             ; {{name}} package
             ["src/{{sanitized}}/handler.clj" (render "handler.clj" data)]
             ["src/{{sanitized}}/model.clj" (render "model.clj" data)]
             ["src/{{sanitized}}/repl.clj" (render "repl.clj" data)]
             ["src/{{sanitized}}/util.clj" (render "util.clj" data)]
             ["src/{{sanitized}}/view.clj" (render "view.clj" data)]
             ; tests
             ["test/{{sanitized}}/test/file_example.jpg" (raw-copy "file_example.jpg", data)]
             ["test/{{sanitized}}/test/events.json" (render "events.json" data)]
             ["test/{{sanitized}}/test/fixtures.clj" (render "fixtures.clj" data)]
             ["test/{{sanitized}}/test/handler.clj" (render "test_handler.clj" data)]
             ["test/{{sanitized}}/test/helpers.clj" (render "helpers.clj" data)]
             ["test/{{sanitized}}/test/util.clj" (render "test_util.clj" data)]
             ; WEB-INF
             ["war-resources/WEB-INF/datastore-indexes.xml" (render "datastore-indexes.xml" data)]
             ["war-resources/WEB-INF/appengine-web.xml" (render "appengine-web.xml" data)]
             ["war-resources/WEB-INF/cron.xml" (render "cron.xml" data)]
             ["war-resources/WEB-INF/logging.properties" (render "logging.properties" data)]
             ["war-resources/WEB-INF/queue.xml" (render "queue.xml" data)]
             ["war-resources/web.xml" (render "web.xml" data)]
             ; root of project
             ["README.md" (render "README.md" data)]
             ["deploy.sh" (render "deploy.sh" data)]
             ["run-dev.sh" (render "run-dev.sh" data)]
             ["pom.xml" (render "pom.xml" data)]
             ["project.clj" (render "project.clj" data)])))
