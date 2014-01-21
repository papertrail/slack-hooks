(ns slack-hooks.service.tender
  (:require [slack-hooks.slack :as slack]
            [clojurewerkz.urly.core :as urly]))

(def tender-base-url
  (System/getenv "TENDER_BASE_URL"))

(def tender-username
  (or
    (System/getenv "TENDER_USERNAME")
    "tender"))

(def tender-avatar
  (System/getenv "TENDER_AVATAR"))

(defn swap-base-url
  "Takes a URL and base URL and returns a new URL that is the original with
  the protocl and host of the base."
  [url base-url]
  (if base-url
    (let [base-url    (urly/url-like base-url)
          protocol    (urly/protocol-of base-url)
          host        (urly/host-of base-url)
          swapped-url (-> url
                          urly/url-like
                          (.mutateProtocol protocol)
                          (.mutateHost host))]
      (str swapped-url))
    url))

(defn internal-message-link
  "Returns an internal URL to the message suitable for support staff."
  [message]
  (str (swap-base-url (:href message) tender-base-url)
       "#comment_"
       (:last-comment-id message)))

(defn extract-system-message
  "Returns the important part of a system message or nil."
  [text]
  (last
    (re-find
      #"(?:The discussion has been|Discussion was) (.*?)\.?$"
      text)))

(defn message-action
  "Returns an action that describes the message."
  [message]
  (let [system-message (:system-message message)]
    (cond (:new-discussion? message) "opened"
          (:resolved? message)       "resolved"
          system-message             system-message
          (and (:internal? message) (not (:system-message? message)))
                                     "updated (internal)"
          :else                      "updated")))


(defn message-from-request
  "Accepts a map of the body of a Tender webhook and returns a map describing
  the message."
  [request]
  (let [payload (:body request)
        {:keys [discussion html_href author_name body number internal
                resolution system_message]} payload
        {:keys [title last_comment_id]} discussion
        extracted-system-message (extract-system-message body)]
    {:href            html_href
     :number          (:number discussion)
     :title           title
     :author          author_name
     :last-comment-id last_comment_id
     :body            body
     :new-discussion? (= 1 number)
     :internal?       internal
     :resolved?       (= true resolution)
     :system-message? system_message
     :system-message  (if system_message extracted-system-message)
     :system-body     (if (and system_message (not extracted-system-message))
                        (str ": " body)
                        "")}))

(defn formatted-message
  "Returns a string describing the given message"
  [request]
  (let [message (message-from-request request)]
    (format "[tender] #%d <%s|\"%s\"> was %s by %s%s"
            (:number message)
            (internal-message-link message)
            (slack/escape (:title message))
            (message-action message)
            (:author message)
            (:system-body message))))

(defn tender
  "Accepts an HTTP request from a Tender webhook and reports the details to a
  Slack webhook."
  [request]
  (let [options {:username tender-username
                 :text (formatted-message request)}]
    (prn request)
    (prn options)
    (slack/notify options)))
