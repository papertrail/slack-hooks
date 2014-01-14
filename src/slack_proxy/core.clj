(ns slack-proxy.core
  (:require slack-proxy.routes
            [ring.adapter.jetty :refer [run-jetty]]))

(defn -main
  ([]
   (let [port (get (System/getenv) "PORT" 3000)]
     (-main port)))
  ([port]
   (let [port (Integer. port)]
     (run-jetty #'slack-proxy.routes/app {:port port}))))