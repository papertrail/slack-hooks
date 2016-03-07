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

(defn slack-escape
  [input]
  (string/escape input
                 {\< "&lt;"
                  \> "&gt;"
                  \& "&amp;"}))

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
    (string/join "\n"
                 (map string/trim-newline
                      (flatten [pre-lines-with-nums center-line-with-num post-lines-with-nums])))))

(defn frame-has-source?
  ([frame]
   (every? frame [:pre_context :context_line :post_context])))

(defn frame->location
  ([{:keys [filename lineno function]}]
   (cond (and filename lineno function)
         (format "%s:%d: in `%s'" filename lineno function)
         (and filename lineno)
         (format "%s:%d" filename lineno))))

(defn frame->source
  ([frame]
   (apply source-with-line-numbers
          (map frame [:lineno :pre_context :context_line :post_context]))))

(defn frame->description
  ([frame]
   (if (frame-has-source? frame)
     (format "%s:\n```%s```" (frame->location frame) (frame->source frame))
     (frame->location frame))))

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
            important-line          (frame->location important-frame)]
        (prn important-frame)
        {:title              (format "*%s: %s: <%s|%s: %s>*" project project-name exception-url
                                     exception-class (slack-escape exception-short-message))
         :description        (frame->description important-frame)
         :simple-description important-line
         }))))

(defn sentry [request]
  (prn "sentry:" (:body request))
  (if-let [data (sentry->slack (:body request))]
    (slack/notify {:slack-url   (or (-> request :params :slack_url) sentry-slack-url)
                   :username    sentry-username
                   :icon_url    sentry-avatar
                   :attachments [{:pretext   (:title data)
                                  :text      (:description data)
                                  :fallback  (:simple-description data)
                                  :color     "#f43f20"
                                  :mrkdwn_in ["text" "pretext"]}]})))
