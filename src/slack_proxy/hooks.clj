(ns slack-proxy.hooks
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-proxy.slack :as slack]))

(defn username-from-email [email]
  (last (re-find #"^([^@]+)" (str email))))

; Build #3877 (17e86c0) of repo/master was successful (107s) https://janky/3877/output
; [build] #1867 (5bbfc24) of repo/master failed http://magnum.travis-ci.com/user/repo...

(defn travis-format [request]
  (let [payload       (-> request :params :payload)
        data          (json/read-str payload :key-fn keyword)
        build-number  (data :number)
        commit        (data :commit)
        compare-url   (data :compare_url)
        build-url     (data :build_url)
        commiter-name (username-from-email (data :committer_email))
        repository    (-> data :repository :name)
        branch        (data :branch)
        status        (lower-case (str (data :result_message)))
        started-at    (data :started_at)
        finished-at   (data :finished_at)]
    (format
      "[build] #%s (<%s|%s>) by %s of %s/%s %s — <%s>"
      build-number commit compare-url commiter-name
      repository branch status build-url)))

(defn tender-internal-url [href]
  (clojure.string/replace href
    "http://help.papertrailapp.com/"
    "https://papertrailapp.tenderapp.com/"))

(defn tender-format [request]
  (let [data              (request :body)
        new-discussion?   (= 1 (data :number))
        number            (-> data :discussion :number)
        discussion-author (-> data :discussion :author_name)
        message-author    (data :author_name)
        title             (get-in data [:discussion :title])
        last-comment-id   (-> data :discussion :last_comment_id)
        href              (str (tender-internal-url (data :html_href)) "#comment_" last-comment-id)
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
  (slack/post {:username "tender"
              :text (tender-format request)}))

(defn travis [request]
  (prn request)
  (slack/post {:username "travis-ci"
               :text (travis-format request)}))

(defn github [request]
  (response (str request)))