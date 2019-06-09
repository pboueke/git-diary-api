(ns git-diary-api.handler
  (:use clj-jgit.porcelain
        )
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs] 
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))


(def config (json/read-str (slurp "config.json")))
(def repo (load-repo (get config "repositoryPath")))
(def url (get config "repositoryUrl"))
(def repo-path (get config "repositoryPath"))
(def sshk-path (get config "sshKeyPath"))
(def name (get config "name"))
(def email (get config "email"))

(defn posts []
  (with-identity {:name sshk-path :exclusive true}
    (git-pull repo)) 
  (json/write-str (map fs/name (fs/list-dir (str repo-path "/posts")))))

(defn new-post [req]
  (let [title (get req "title")
        body (get req "body")
        filename (str (.format (new java.text.SimpleDateFormat "yyyy-MM-dd-") (java.util.Date.))
                      (clojure.string/replace title #" " "-") ".md")
        file-url (str url "/tree/master/posts/" filename)]
    (with-identity {:name sshk-path :exclusive true}
      (git-pull repo))
    (spit (str repo-path "/posts/" filename) (str "# " title "\n\n" body))
    (git-add repo (str "posts/" filename))
    (git-commit repo (str "Add file " filename) {:name name :email email})
    (with-identity {:name sshk-path :exclusive true} 
      (git-push repo))
    (json/write-str (list file-url))))
    
(defn get-post [name]
  (slurp (str repo-path "/posts/" name ".md")))


(defroutes app-routes
  (GET "/" [] "Up and running!")
  (GET "/posts" [] (posts))
  (PUT "/post/new" request (new-post (json/read-str (slurp (:body request)))))
  (GET "/post/:name" [name] (get-post name))
  (route/not-found "Route not found!"))

(def app
  (wrap-defaults app-routes api-defaults))
