(ns {{name}}.view
  (:require [hiccup.page :as page]
    [{{name}}.auth :as auth]
    [{{name}}.push-queue :as pq]
    [gaeclj.appidentity :refer [sdk-version app-version
                           application-id environment
                           last-deployed-datetime]]
    [gaeclj.env :as env]
    [clj-time.format :as f]
    [clj-time.core :as t]))

(defn- module-hostname [request]
  (or (pq/get-module-hostname)
    (get-in request [:headers "host"])))

(defn- tr [type & cols]
  (cons [:tr] (map #(vector type %) cols)))

(def ^:private formatter (f/with-zone 
                           (f/formatters :rfc822)
                           (t/time-zone-for-id "America/Chicago")))

(defn base [request title heading & more]
  (page/html5 
   [:head
    [:title title]
    (page/include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
    (page/include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")]
   [:body
    [:ul {:class "nav nav-pills"}
     [:li {:role "presentation" :class "active"} [:a {:href "/"} "Home"]]
     (if (auth/user-logged-in?)
       [:li {:role "presentation" :class "active"} [:a {:href (auth/create-logout-url "/")} "Log Out"]]
       [:li {:role "presentation" :class "active"} [:a {:href (auth/create-login-url "/")}  "Log In"]])
     ]
    [:div {:class "container"}
     [:h1 heading]
     (when (seq more)
       more)]
    (page/include-js "//code.jquery.com/jquery-3.1.1.min.js")
    (page/include-js "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")]))

(defn home [request]  
  (base request "{{name}} App Engine App" "{{name}}"
        (if (auth/user-admin?)
          [:div {:class "panel panel-default"}
           [:div {:class "panel-heading"} "App Identity API values"]
           [:div {:class "panel-body"} [:p "Remember dev-appserver has stubbed versions of these. In prod this information is a lot more interesting."]]
           [:table {:class "table table-bordered table-striped table-condensed"}
            (tr :th "Property" "Value")
            (tr :td "Application Id" (application-id))
            (tr :td "Version" (app-version))
            (tr :td "Module Hostname" (module-hostname request))
            (tr :td "GCS Bucket" env/gcs-bucket-name)
            (tr :td "Last Deployed At" (f/unparse formatter (last-deployed-datetime)))
            (tr :td "Environment" (environment))
            (tr :td "SDK Version" (sdk-version))]
           ]
          [:h3 "Welcome..."])))

(defn file-upload-form [request & more]
  (base request "{{name}} | file upload example" "File Upload Example"
        [:form {:action "/save" :method "post" :enctype "multipart/form-data"}
         [:input {:name "thefile" :type "file"}]
         [:input {:type "submit" :name "submit" :value "submit"}]]
        (when (seq more)
          more)))
