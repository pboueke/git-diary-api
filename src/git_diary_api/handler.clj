(ns git-diary-api.handler
  (:use clj-jgit.porcelain
        )
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs] 
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def config (json/read-str (slurp "config.json")))
(def repo (load-repo (get config "repositoryPath")))
(def repo-path (get config "repositoryPath"))
(def sshk-path (get config "sshKeyPath"))

(defn posts []
  (with-identity {:name sshk-path :exclusive true}
    (git-pull repo)
    ) 
    (json/write-str (map fs/name (fs/list-dir repo-path)))      
  )


(defroutes app-routes
  (GET "/" [] "Hello World 2")
  (GET "/posts" [] (posts))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
