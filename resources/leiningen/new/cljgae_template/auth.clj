(ns {{name}}.auth
    (:import [com.google.appengine.api.users UserService UserServiceFactory]))

(defn juser-service-factory []
  (UserServiceFactory/getUserService))

(defn jcurrent-user []
  (.getCurrentUser (juser-service-factory)))

(defn user-logged-in? []
  (.isUserLoggedIn (juser-service-factory)))

(defn user-admin? []
  (if (user-logged-in?)
    (.isUserAdmin (juser-service-factory))
    false))

(defn create-login-url
  ([destination-url] (.createLoginURL (juser-service-factory) destination-url))
  ([destination-url auth-domain] (.createLoginURL (juser-service-factory) destination-url auth-domain))
  ([destination-url auth-domain fed-identity attributes-from-req]
   (.createLoginURL (juser-service-factory) destination-url auth-domain fed-identity attributes-from-req)))

(defn create-logout-url
  ([destination-url] (.createLogoutURL (juser-service-factory) destination-url))
  ([destination-url auth-domain] (.createLogoutURL (juser-service-factory) destination-url auth-domain)))

