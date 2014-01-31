(ns slack-hooks.service.papertrail
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]))
            ; [clojure.string :as str]))

(def papertrail-username
  (or
    (System/getenv "PAPERTRAIL_USERNAME")
    "papertrail"))

(def papertrail-avatar
  (System/getenv "PAPERTRAIL_AVATAR"))

(def papertrail-channel
  (System/getenv "PAPERTRAIL_CHANNEL"))

(defn formatted-message
  [request]
  (prn (-> request :form-params (get "payload")))
  (let [payload     (-> request :form-params (get "payload"))
        data        (json/read-str payload :key-fn keyword)
        href        (get-in data [:saved_search :html_search_url])
        label       (get-in data [:saved_search :name])
        event-count (count (:events data))]
    (format "Search <%s|\"%s\"> found %d matches"
            href, label, event-count)))

(defn papertrail
  "Accepts an HTTP request from a Papertrail webhook and reports the details
  to a Slack webhook."
  [request]
    (prn request)
  (let [options {:username papertrail-username
                 :icon_url papertrail-avatar
                 :channel  papertrail-channel
                 :text     (formatted-message request)}]
    (prn options)
    (slack/notify options)))
