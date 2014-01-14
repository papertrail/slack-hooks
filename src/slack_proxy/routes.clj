(ns slack-proxy.routes
  (:use slack-proxy.hooks
        ring.util.response
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]))

(defn four-oh-four [request]
  (-> (response "Page not found")
      (status 404)))

(def routes-by-uri
  {"/github" github
   "/travis" travis})

(defn handler [request]
  ((routes-by-uri (:uri request) four-oh-four) request))

(def app
  (-> handler
      wrap-keyword-params
      wrap-params))
