(ns slack-hooks.service.sentry
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clojure.string :as string]))

(def sentry-username
   (or
     (System/getenv "SENTRY_USERNAME")
     "sentry"))

(def sentry-avatar
  (or
    (System/getenv "SENTRY_AVATAR")
    "https://slack.global.ssl.fastly.net/66f9/img/services/sentry_128.png"))

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

(defn source-with-line-numbers
  [center-line-number pre-lines center-line post-lines]
  (let [pre-line-count     (count pre-lines)
        post-line-count    (count post-lines)
        max-line-number    (+ center-line-number post-line-count)
        min-line-number    (- center-line-number pre-line-count)
        line-number-length (count (format "%d" max-line-number))
        format-string      (str "%1s %" line-number-length "d %s")
        pre-line-nums      (range min-line-number center-line-number)
        post-line-nums     (range (+ 1 center-line-number) (+ 1 max-line-number))

        pre-lines-with-nums (map #(format format-string "" %1 %2) pre-line-nums pre-lines)
        post-lines-with-nums (map #(format format-string "" %1 %2) post-line-nums post-lines)
        center-line-with-num (format format-string ">" center-line-number center-line)]
    (string/join "" (flatten [pre-lines-with-nums center-line-with-num post-lines-with-nums]))))

(defn sentry->slack [message]
  (let [project                 (:project message)
        project-name            (:project_name message)
        exception-url           (:url message)
        exception               (-> message :event :sentry.interfaces.Exception :values first)
        important-frame         (last (filter :in_app (-> exception :stacktrace :frames)))]
    (if (and exception important-frame)
      (let [exception-class         (:type exception)
            exception-message       (:value exception)
            exception-short-message (truncate-string (first (string/split-lines (str exception-message))) 100)

            important-source            (string/join (flatten (map important-frame [:pre_context :context_line :post_context])))
            important-source-with-lines (apply source-with-line-numbers (map important-frame [:lineno :pre_context :context_line :post_context]))
            important-line              (apply format "%s:%d: in `%s':" (map important-frame [:filename :lineno :function]))]
        {:title              (format "*%s: %s: <%s|%s: %s>*" project project-name exception-url
                                     exception-class exception-short-message)
         :description        (format "%s\n```%s```" important-line important-source-with-lines)
         :simple-description important-line
         }))))

(defn sentry [request]
  (let [data (sentry->slack (:body request))]
    (prn "sentry:" (:body request))
    (slack/notify {:slack-url   (or (-> request :params :slack_url) sentry-slack-url)
                   :username    sentry-username
                   :icon_url    sentry-avatar
                   :attachments [{:pretext   (:title data)
                                  :text      (:description data)
                                  :fallback  (:simple-description data)
                                  :color     "#f43f20"
                                  :mrkdwn_in ["text" "pretext"]}]})))
