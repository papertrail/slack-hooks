(ns slack-hooks.service.travis
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.string :as str]))

(def travis-username
  (or
    (System/getenv "TRAVIS_USERNAME")
    "travis-ci"))

(def travis-avatar
    (System/getenv "TRAVIS_AVATAR"))

(defn status-color
  [request]
  (let [payload (-> request :params :payload)
        data    (json/read-str payload :key-fn keyword)
        passing (= (data :result) 0)]
    (if passing
      "good"
      "danger")))

(defn username-from-email [email]
  (last (re-find #"^([^@]+)" (str email))))

(defn pretty-duration
  "Give a duration in a nice 2m32s format"
  [seconds]
  (let [minutes           (quot seconds 60)
        remaining-seconds (mod seconds 60)]
    (cond
      (= minutes 0)           (str seconds "s")
      (= remaining-seconds 0) (str minutes "m")
      :else                   (str minutes "m" remaining-seconds "s"))))

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
        build-number    (:number data)
        commit          (:commit data)
        short-commit    (subs commit 0 8)
        compare-url     (:compare_url data)
        pull-request    (last (re-find #"/(pull/\d+)$" compare-url))
        build-url       (:build_url data)
        commiter-name   (username-from-email (:committer_email data))
        repository      (-> data :repository :name)
        branch          (:branch data)
        repository-url  (str (-> data :repository :url)
                             "/tree/"
                             branch)
        repo-and-branch (if pull-request
                          (str repository "/" pull-request)
                          (str repository "/" branch))
        status          (str/lower-case (str (:result_message data)))
        started-at      (:started_at data)
        finished-at     (:finished_at data)
        duration        (pretty-duration (duration started-at finished-at))]
    (format
      "[build] <%s|#%s> (<%s|%s>) by %s of <%s|%s> %s in %s"
      build-url build-number compare-url short-commit commiter-name
      repository-url repo-and-branch status duration)))


(defn travis [request]
  (prn (-> request :params :payload))
  (slack/notify {:username travis-username
                 :icon_url travis-avatar
                 :attachments [{:text (travis-format request)
                               :color (status-color request)}]}))
