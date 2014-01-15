(ns slack-hooks.service.tender
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clojurewerkz.urly.core :as urly]))

(defn swap-base-url
  "Takes a URL and base URL and returns a new URL that is the original with
  the protocl and host of the base."
  [url base-url]
  (if base-url
    (let [base-url (urly/url-like base-url)
          protocol (urly/protocol-of base-url)
          host (urly/host-of base-url)
          swapped-url (-> url
                          urly/url-like
                          (.mutateProtocol protocol)
                          (.mutateHost host))]
      (str swapped-url))
    url))

(defn internal-message-link
  "Returns an internal URL to the message suitable for support staff."
  ([message]
   (internal-message-link message (System/getenv "TENDER_BASE_URL")))
  ([message base-url]
   (str (swap-base-url (:href message) base-url)
        "#comment_"
        (:last-comment-id message))))

(defn message-action
  "Returns an action that describes the message."
  [message]
  (cond (:new-discussion? message) "opened"
        (:resolved? message) "resolved"
        (and (:internal? message)
             (not (:system-message? message))) "updated (internal)"
        :else "updated"))

(defn message-from-request-body
  "Accepts a map of the body of a Tender webhook and returns a map describing
  the message."
  [request-body]
  (let [discussion (request-body :discussion)]
    {:href (request-body :html_href)
     :number (discussion :number)
     :title (discussion :title)
     :author (request-body :author_name)
     :last-comment-id (discussion :last_comment_id)
     :body (request-body :body)
     :new-discussion? (= 1 (request-body :number))
     :internal? (request-body :internal)
     :resolved? (= true (request-body :resolution))
     :system-message? (request-body :system_message)}))

(defn formatted-message [request]
  (let [message (message-from-request-body (:body request))
        extras (if (and (:system-message? message)
                        (not (:resolved? message)))
                 (str ": " (:body message))
                 "")]
    (format "[tender] #%d \"<%s|%s>\" was %s by %s%s"
            (:number message)
            (internal-message-link message)
            (:title message)
            (message-action message)
            (:author message)
            extras)))

(defn tender
  []
  (let [base-url (System/getenv "TENDER_BASE_URL")
        username (get (System/getenv) "TENDER_USERNAME" "tender")
        avatar-url (System/getenv "TENDER_AVATAR_URL")]
    (fn [request]
      (let [options {:username username
                     :avatar-url avatar-url
                     :text (formatted-message request)}]
        (slack/notify options)))))
