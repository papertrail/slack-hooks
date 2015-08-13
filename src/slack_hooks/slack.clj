(ns slack-hooks.slack
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def slack-url
  (System/getenv "SLACK_URL"))

(defn escape
  [message]
  (clojure.string/escape message {\< "&lt;" \> "&gt;"}))

(defn link-to
  [text url]
  (if url
    (format "<%s|%s>" url text)
    text))

(defn notify [data]
  (let [post-url (or (:slack-url data) slack-url)
        slack-data (dissoc data :slack-url)]
    (client/post post-url
                 {:insecure? true  ;; Temporary until we're able to figure out how to
                  ;; properly validate Slack's SSL cert.
                  :content-type :json
                  :form-params slack-data})))
