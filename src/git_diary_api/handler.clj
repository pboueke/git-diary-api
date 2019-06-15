(ns git-diary-api.handler
  (:use clj-jgit.porcelain
        )
  (:require [clojure.data.json :as json]
            [schema.core :as s]
            [me.raynes.fs :as fs]
            [compojure.api.sweet :refer :all]
            [compojure.api.meta :refer [restructure-param]]
            [ring.util.http-response :refer :all]
            [buddy.auth.backends :as backends]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))

;; Configuration
(def config (json/read-str (slurp "config.json")))
(def repo (load-repo (get config "repositoryPath")))
(def url (get config "repositoryUrl"))
(def repo-path (get config "repositoryPath"))
(def sshk-path (get config "sshKeyPath"))
(def name (get config "name"))
(def email (get config "email"))
(def tokens {(keyword (get config "apiToken")) :admin})

;; Authorization
(defn my-authfn
  [req token]
  (let [token (keyword token)]
    (get tokens token nil)))

(def backend
  (backends/token
   {:authfn my-authfn}))

(defn access-error [req val]
  {:status 401 :headers {} :body "Unauthorized"})

(defn wrap-rule [handler rule]
  (-> handler
      (wrap-access-rules {:rules [{:pattern #".*"
                                   :handler rule}]
                          :on-error access-error})))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-rule rule]))

;; Utilities
(s/defschema Post
  {:title s/Str
   :body s/Str})

(defn re-index []
  (let [index-path  "posts/index.json"]
    (spit (str repo-path "/" index-path) (json/write-str (map fs/name (fs/list-dir (str repo-path "/posts")))))
    (git-add repo index-path)))

;; Routes
(defn posts []
  (println "Retrieving posts...")
  (with-identity {:name sshk-path :exclusive true}
    (git-pull repo))
  (json/write-str (filter (fn [x] (not (= x "index"))) (map fs/name (fs/list-dir (str repo-path "/posts"))))))

(defn new-post [title body]
  (println "Creating new post with title " title)
  (let [filename (str (.format (new java.text.SimpleDateFormat "yyyy-MM-dd-") (java.util.Date.))
                      (clojure.string/replace title #" " "-") ".md")
        file-url (str url "/tree/master/posts/" filename)]
    (with-identity {:name sshk-path :exclusive true}
      (git-pull repo))
    (spit (str repo-path "/posts/" filename) (str "# " title "\n\n" body))
    (git-add repo (str "posts/" filename))
    (re-index)
    (git-commit repo (str "Add file " filename) {:name name :email email})
    (with-identity {:name sshk-path :exclusive true} 
      (git-push repo))
    (json/write-str (list file-url))))
    
(defn get-post [name]
  (println "Retrieving post " name)
  (with-identity {:name sshk-path :exclusive true}
    (git-pull repo))
  (slurp (str repo-path "/posts/" (clojure.string/replace name #"_comma_" ",") ".md")))

(defn get-post-link [name]
  (println "Retrieving post link for " name)
  (json/write-str (list (str url "/tree/master/posts/" (clojure.string/replace name #"_comma_" ",") ".md"))))

(defn delete-post [name]
  (println "Deleting post " name)
  (let [filename (str (clojure.string/replace name #"_comma_" ",") ".md")]
    (fs/delete (str repo-path "/posts/" filename))
    (git-rm repo (str "posts/" filename))
    (re-index)
    (git-commit repo (str "Remove file " filename) {:name name :email email})
    (with-identity {:name sshk-path :exclusive true}
      (git-push repo))
    (json/write-str (list name))))

;; App
(def api-app
  (api
   (GET "/" [] "Up and running!")
   (GET "/posts" [] 
     :auth-rules authenticated? 
     (posts))
   (PUT "/post/new" request 
     :auth-rules authenticated? 
     :body [{:keys [title body]} Post]
     (new-post title body))
   (GET "/post/:name" [name] 
     :auth-rules authenticated? 
     (get-post name))
   (GET "/post/:name/link" [name]
     :auth-rules authenticated?
     (get-post-link name))
   (DELETE "/post/:name" [name] 
     :auth-rules authenticated? 
     (delete-post name))))

(def app (-> api-app
             (wrap-authentication backend)))
