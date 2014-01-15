(ns slack-hooks.slack
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def slack-url
  (System/getenv "SLACK_URL"))

(defn escape-message
  [message]
  (clojure.string/escape message {\< "&lt;" \> "&gt;"}))

(defn notify [data]
  (client/post slack-url
    {:insecure? true  ;; Temporary until we're able to figure out how to
                      ;; properly validate Slack's SSL cert.
     :content-type :json
     :form-params data}))
