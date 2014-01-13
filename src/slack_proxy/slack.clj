(ns slack-proxy.slack
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def slack-url
  (System/getenv "SLACK_URL"))

(defn post [data]
  (client/post
    slack-url
    {:content-type :json
     :form-params data}))
