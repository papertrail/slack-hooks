(ns slack-hooks.slack
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def slack-url
  (System/getenv "SLACK_URL"))

(defn escape-message
  [data]
  (let [escaped (clojure.string/escape (:text data)
                                       {\< "&lt;"
                                        \> "&gt;"})]
    (assoc data :text escaped)))

(defn notify [data]
  (client/post slack-url
    {:insecure? true  ;; Temporary until we're able to figure out how to
                      ;; properly validate Slack's SSL cert.
     :content-type :json
     :form-params (escape-message data)}))
