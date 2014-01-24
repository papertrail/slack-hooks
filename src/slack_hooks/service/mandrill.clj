(ns slack-hooks.service.mandrill
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.string :as str]))

(def mandrill-username
  (or
    (System/getenv "MANDRILL_USERNAME")
    "mandrill"))

(def mandrill-avatar
  (System/getenv "MANDRILL_AVATAR"))

(def mandrill-channel
  (System/getenv "MANDRILL_CHANNEL"))

(def customer-search-base-url
  (System/getenv "CUSTOMER_SEARCH_BASE_URL"))

(defn customer-href
  [email]
  (str customer-search-base-url email))

(def event-description
  {"reject" "rejected"
   "hard_bounce" "hard bounced"
   "soft_bounce" "soft bounced"
   "spam" "flagged as spam"})

(defn formatted-message
  [request]
  (let [payload (-> request :params :mandrill_events)]
    (for [event (json/read-str payload :key-fn keyword)
          :let [description (get event-description (:event event))
                message     (:msg event)
                recipient   (:email message)
                sender      (:sender message)
                subject     (:subject message)]]
      (format "Email to <%s|%s> from %s was %s: \"%s\""
              (customer-href recipient)
              recipient sender description subject))))

(defn mandrill
  "Accepts an HTTP request from a Mandrill webhook and reports the details to
  a Slack webhook."
  [request]
  (let [base-options {:username mandrill-username
                      :icon_url mandrill-avatar
                      :channel  mandrill-channel}]
    (prn request)
    (for [text (formatted-message request)
          :let [options (assoc base-options :text text)]]
      (do
        (prn options)
        (slack/notify options)))))
