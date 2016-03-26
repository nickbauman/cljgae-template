(ns leiningen.new.cljgae-template
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "cljgae-template"))

(defn cljgae-template
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :year (+ 1900 (.getYear (new java.util.Date)))
              :sanitized (name-to-path name)}]
    (main/info (str "Generating new cljgae-template project '" name "'"))
    (->files data
             ; {{name}} package
             ["src/{{sanitized}}/appengine.clj" (render "appengine.clj" data)]
             ["src/{{sanitized}}/db.clj" (render "db.clj" data)]
             ["src/{{sanitized}}/env.clj" (render "env.clj" data)]
             ["src/{{sanitized}}/gcs.clj" (render "gcs.clj" data)]
             ["src/{{sanitized}}/handler.clj" (render "handler.clj" data)]
             ["src/{{sanitized}}/model.clj" (render "model.clj" data)]
             ["src/{{sanitized}}/push_queue.clj" (render "push_queue.clj" data)]
             ["src/{{sanitized}}/repl.clj" (render "repl.clj" data)]
             ["src/{{sanitized}}/util.clj" (render "util.clj" data)]
             ["src/{{sanitized}}/view.clj" (render "view.clj" data)]
             ; tests
             ["test/{{sanitized}}/test/fixtures.clj" (render "fixtures.clj" data)]
             ["test/{{sanitized}}/test/appengine.clj" (render "test_appengine.clj" data)]
             ["test/{{sanitized}}/test/db.clj" (render "test_db.clj" data)]
             ["test/{{sanitized}}/test/handler.clj" (render "test_handler.clj" data)]
             ["test/{{sanitized}}/test/push_queue.clj" (render "test_push_queue.clj" data)]
             ["test/{{sanitized}}/test/util.clj" (render "test_util.clj" data)]
             ; WEB-INF
             ["war-resources/WEB-INF/appengine-web.xml" (render "appengine-web.xml" data)]
             ["war-resources/WEB-INF/cron.xml" (render "cron.xml" data)]
             ["war-resources/WEB-INF/logging.properties" (render "logging.properties" data)]
             ["war-resources/WEB-INF/queue.xml" (render "queue.xml" data)]
             ["war-resources/web.xml" (render "web.xml" data)]
             ; root of project
             ["README.md" (render "README.md" data)]
             ["deploy.sh" (render "deploy.sh" data)]
             ["run-dev.sh" (render "run-dev.sh" data)]
             ["project.clj" (render "project.clj" data)])))
