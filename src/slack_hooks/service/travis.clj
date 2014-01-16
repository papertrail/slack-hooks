(ns slack-hooks.service.travis
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(def travis-username
  (get (System/getenv) "TRAVIS_USERNAME" "travis-ci"))

(def travis-avatar
  (System/getenv "TRAVIS_AVATAR"))

(defn username-from-email [email]
  (last (re-find #"^([^@]+)" (str email))))

(defn pretty-duration
  "Give a duration in a nice 2m32s format"
  [seconds]
  (let [minutes           (int (Math/floor (/ seconds 60)))
        remaining-seconds (mod seconds 60)]
    (if (= minutes 0)
      (str seconds "s")
      (str minutes "m" remaining-seconds "s"))))

(defn duration
  "Calculate the number of seconds between two times"
  [started-at finished-at]
  (time/in-seconds
    (time/interval
      (time-format/parse started-at)
      (time-format/parse finished-at))))


; Build #3877 (17e86c0) of repo/master was successful (107s) https://janky/3877/output
; [build] #1867 (5bbfc24) of repo/master failed http://magnum.travis-ci.com/user/repo...

(defn travis-format [request]
  (let [payload         (-> request :params :payload)
        data            (json/read-str payload :key-fn keyword)
        build-number    (data :number)
        commit          (data :commit)
        short-commit    (subs commit 0 8)
        compare-url     (data :compare_url)
        pull-request    (last (re-find #"/(pull/\d+)$" compare-url))
        build-url       (data :build_url)
        commiter-name   (username-from-email (data :committer_email))
        repository      (-> data :repository :name)
        repository-url  (-> data :repository :url)
        branch          (data :branch)
        repo-and-branch (if pull-request
                          (str repository "/" pull-request)
                          (str repository "/" branch))
        status          (lower-case (str (data :result_message)))
        started-at      (data :started_at)
        finished-at     (data :finished_at)
        duration        (pretty-duration (duration started-at finished-at))]
    (format
      "[build] #%s (<%s|%s>) by %s of <%s|%s> %s in %s — <%s>"
      build-number compare-url short-commit commiter-name
      repository-url repo-and-branch status duration
      build-url)))


(defn travis [request]
  (prn (-> request :params :payload))
  (slack/notify {:username travis-username
                 :icon_url travis-avatar
                 :text (travis-format request)}))
