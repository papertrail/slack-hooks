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

(defn tender-format [request]
  (let [data              (request :body)
        new-discussion?   (= 1 (data :number))
        number            (-> data :discussion :number)
        discussion-author (-> data :discussion :author_name)
        message-author    (data :author_name)
        title             (get-in data [:discussion :title])
        last-comment-id   (-> data :discussion :last_comment_id)
        href              (str (convert-to-internal-url (data :html_href))
                               "#comment_"
                               last-comment-id)
        internal?         (data :internal)
        resolved?         (data :resolution)
        system-message?   (data :system_message)
        body              (data :body)

        action            (cond new-discussion? "opened"
                                resolved?       "resolved"
                                (and internal? (not system-message?))
                                "updated (internal)"
                                :else           "updated")
        extras            (if (and system-message? (not resolved?))
                            (str ": " body)
                            "")]

    (format "[tender] #%d \"<%s|%s>\" was %s by %s%s"
            number href title action message-author extras)))

(defn tender [request]
  (prn request)
  (slack/notify {:username "tender"
                 :text (tender-format request)}))

