(ns slack-proxy.slack
  (:require [clj-http.client :as client]))


(def slack-url (System/getenv "SLACK_URL"))

(defn notify [data]
  (client/post
    slack-url
    {:insecure? true  ;; Temporary until we're able to figure out how to
                      ;; properly validate Slack's SSL cert.
     :content-type :json
     :form-params data}))
