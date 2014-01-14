(ns slack-hooks.core
  (:require slack-hooks.routes
            [ring.adapter.jetty :refer [run-jetty]]))

(defn -main
  ([]
   (let [port (get (System/getenv) "PORT" 3000)]
     (-main port)))
  ([port]
   (let [port (Integer. port)]
     (run-jetty #'slack-hooks.routes/app {:port port}))))
