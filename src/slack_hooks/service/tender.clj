(ns slack-hooks.service.tender
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clojurewerkz.urly.core :as urly]))

(defn convert-to-internal-url
  "Converts a public Tender discussion URL into an internal URL suitable for
  support staff."
  ([discussion-url]
   (let [base-url (System/getenv "TENDER_BASE_URL")]
     (convert-to-internal-url discussion-url base-url)))

  ([discussion-url base-url]
   (if base-url
     (let [base-url (urly/url-like base-url)
           protocol (urly/protocol-of base-url)
           host (urly/host-of base-url)
           discussion-url (-> discussion-url
                              urly/url-like
                              (.mutateProtocol protocol)
                              (.mutateHost host))]
       (str discussion-url))
     discussion-url)))

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

(defn tender-format [request]
  (let [message (message-from-request-body (:body request))
        href (str (convert-to-internal-url (:href message))
                  "#comment_"
                  (:last-comment-id message))
        action (cond (:new-discussion? message) "opened"
                     (:resolved? message) "resolved"
                     (and (:internal? message)
                          (not (:system-message? message)))
                       "updated (internal)"
                     :else "updated")
        extras (if (and (:system-message? message)
                        (not (:resolved? message)))
                 (str ": " (:body message))
                 "")]
    (format "[tender] #%d \"<%s|%s>\" was %s by %s%s"
            (:number message)
            href
            (:title message)
            action
            (:author message)
            extras)))

(defn tender
  [request]
  (let [options {:username "tender"
                :text (tender-format request)}]
    (prn (tender-format request))))
    ; (slack/notify options)))
