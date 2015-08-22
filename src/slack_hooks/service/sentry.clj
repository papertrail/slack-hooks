(ns slack-hooks.service.sentry
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clojure.string :as string]))

(def sentry-username
     (or
       (System/getenv "SENTRY_USERNAME")
       "sentry"))

(def sentry-avatar
     (System/getenv "SENTRY_AVATAR"))

(def sentry-slack-url
     (System/getenv "SENTRY_SLACK_URL"))

(defn truncate-string
  ([string length suffix]
   (let [string-length (count string)
         suffix-length (count suffix)]

     (if (< string-length length)
       string
       (str (subs string 0 (- length suffix-length)) suffix))))

  ([string length]
   (truncate-string string length "...")))

(defn sentry->slack [message]
  (let [project                 (:project message)
        project-name            (:project_name message)
        exception-url           (:url message)
        exception               (-> message :event :sentry.interfaces.Exception :values first)
        important-frame         (first (filter :in_app (-> exception :stacktrace :frames)))]
    (if (and exception important-frame)
      (let [exception-class         (:type exception)
            exception-message       (:value exception)
            exception-short-message (truncate-string (first (string/split-lines (str exception-message))) 100)

            important-source        (string/join (map important-frame [:pre_context :context_line :post_context]))
            important-line          (apply format "%s:%d: in `%s'" (map important-frame [:filename :lineno :function]))]
        {:title       (format "%s: %s: <%s|%s: %s>" project project-name exception-url
                              exception-class exception-short-message)
         :description (format "%s\n```%s```" important-line important-source)
         }))))

(defn sentry [request]
  (let [data (sentry->slack (:body request))]
    ; (prn (-> request :params :payload))
    (slack/notify {:slack-url   (or (-> request :params :slack_url) sentry-slack-url)
                   :username    sentry-username
                   :icon_url    sentry-avatar
                   :attachments [{:pretext (:title data)
                                  :text    (:description data)
                                  :color   "danger"}]})))
