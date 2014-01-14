(ns slack-hooks.service.travis
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]))


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


(defn travis [request]
  (prn request)
  (slack/notify {:username "travis-ci"
                 :text (travis-format request)}))
