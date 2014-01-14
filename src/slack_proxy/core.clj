(ns slack-proxy.core
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.util.response :refer [response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [slack-proxy.slack :as slack]))

; "Reduce spend on PT" has been updated by Troy http://help.papertrailapp.com/discussions/email/8088
(defn handler [request]
  (let [params       (:params request)
        username     (or (get-in params ["user" "name"])
                         (params "author_name"))
        title        (get-in params ["discussion" "title"])
        href         (params "html_href")
        https-href   href
        linked-title (format "<%s|%s>" https-href, title)
        public?      (not (params "internal"))]
    (slack/notify {"fallback" linked-title
                   ; "channel" "#test"
                   "fields" [{"title" "Title"
                              "value" linked-title}
                             {"title" "Name"
                              "value" username
                              "short" true}
                             {"title" "Public?"
                              "value" (if public? "Yes" "No")
                              "short" true}]}))
  (response "Done\n"))

(def app
  (-> handler
      wrap-json-params
      wrap-json-response))

(defn -main
  ([]
   (let [port (get (System/getenv) "PORT" 3000)]
     (-main port)))
  ([port]
   (let [port (Integer. port)]
     (run-jetty #'app {:port port}))))
