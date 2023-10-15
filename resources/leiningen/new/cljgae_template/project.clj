(def appengine-version "2.0.20")

(defproject {{name}} "0.1.0-SNAPSHOT"
            :description "{{name}} short description FIXME"
            :url "http://{{name}}.appspot.com/"             ; make sure this is a real project and has been initialized to the java8 runtime
            :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
            :dependencies [[org.clojure/clojure "1.10.1"]
                           [compojure "1.6.1"]
                           [hiccup "1.0.5"]
                           [ring-server "0.5.0"]
                           [ring/ring-defaults "0.3.2"]
                           [org.clojure/tools.logging "1.0.0"]
                           [org.clojure/data.json "1.0.0"]
                           [ch.qos.logback/logback-classic "1.2.3"]
                           [com.google.guava/guava "23.0"]
                           [com.google.appengine/appengine-api-1.0-sdk ~appengine-version]
                           [com.google.appengine.tools/appengine-gcs-client "0.8"
                            :exclusions [com.google.api-client/google-api-client
                                         com.google.appengine/appengine-api-1.0-sdk
                                         com.google.guava/guava
                                         com.google.http-client/google-http-client-jackson2
                                         com.google.api-client/google-api-client-appengine
                                         com.google.http-client/google-http-client]]
                           [org.apache.httpcomponents/httpclient "4.5.14"]
                           [com.google.api-client/google-api-client-appengine "1.30.9"
                            :exclusions [com.google.guava/guava-jdk5]]
                           [com.google.oauth-client/google-oauth-client-appengine "1.30.6"
                            :exclusions [com.google.guava/guava-jdk5]]
                           [com.google.http-client/google-http-client-appengine "1.34.2"
                            :exclusions [com.google.guava/guava-jdk5]]
                           [gaeclj-pq "0.1.2"]
                           [gaeclj-auth "0.1.0"]
                           [gaeclj-ds "0.1.3.1"]
                           [gaeclj-gcs "0.1.3"]]

            :java-source-paths ["src-java"]
            :plugins [[lein-ring "0.12.5"]]
            :ring {:handler {{name}}.handler/app
                            :init {{name}}.handler/init
                   :destroy {{name}}.handler/destroy
                            :web-xml "war-resources/web.xml"}
            :aot :all
            :profiles
            {:production
             {:ring
              {:open-browser? false, :stacktraces? false, :auto-reload? false}}
             :dev
             {:dependencies [[ring/ring-mock "0.4.0"]
                             [ring/ring-devel "1.8.0"]
                             [com.google.appengine/appengine-testing ~appengine-version]
                             [com.google.appengine/appengine-api-stubs ~appengine-version]
                             [com.google.appengine/appengine-tools-sdk ~appengine-version]]}})
