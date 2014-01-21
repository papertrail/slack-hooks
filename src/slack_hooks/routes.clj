(ns slack-hooks.routes
  (:use ring.util.response)
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-body]]
            [slack-hooks.service.tender :as service.tender]
            [slack-hooks.service.travis :as service.travis]
            [slack-hooks.service.github :as service.github]))

(defn four-oh-four [request]
  (-> (response "Page not found")
      (status 404)))

(def routes-by-uri
  {"/github" service.github/github
   "/travis" service.travis/travis
   "/tender" service.tender/tender})

(defn handler [request]
  ((routes-by-uri (:uri request) four-oh-four) request))

(def app
  (-> handler
      wrap-keyword-params
      (wrap-json-body {:keywords? true})
      wrap-params))
