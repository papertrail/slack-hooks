(ns slack-hooks.routes
  (:use ring.util.response)
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-body]]
            [slack-hooks.service.github     :as service.github]
            [slack-hooks.service.mandrill   :as service.mandrill]
            [slack-hooks.service.opsgenie   :as service.opsgenie]
            [slack-hooks.service.papertrail :as service.papertrail]
            [slack-hooks.service.tender     :as service.tender]
            [slack-hooks.service.travis     :as service.travis]))

(defn four-oh-four [request]
  (-> (response "Page not found")
      (status 404)))

(def routes-by-uri
  {"/github"     service.github/github
   "/mandrill"   service.mandrill/mandrill
   "/opsgenie"   service.opsgenie/opsgenie
   "/papertrail" service.papertrail/papertrail
   "/tender"     service.tender/tender
   "/travis"     service.travis/travis})

(defn handler [request]
  (if (= :head (:request-method request))
    (response "ok")
    (if-let [route (routes-by-uri (:uri request) four-oh-four)]
      (if (route request)
        (response "ok")
        (-> (response "fail")
            (status 500)))
      (four-oh-four))))

(def app
  (-> handler
      wrap-keyword-params
      (wrap-json-body {:keywords? true})
      wrap-params))
