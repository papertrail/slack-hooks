(ns slack-proxy.routes
  (:use slack-proxy.hooks
        ring.util.response
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.json :only [wrap-json-body]]))

(defn four-oh-four [request]
  (-> (response "Page not found")
      (status 404)))

(def routes-by-uri
  {"/github" github
   "/travis" travis
   "/tender" tender})

(defn handler [request]
  ((routes-by-uri (:uri request) four-oh-four) request))

(def app
  (-> handler
      wrap-keyword-params
      (wrap-json-body {:keywords? true})
      wrap-params))
