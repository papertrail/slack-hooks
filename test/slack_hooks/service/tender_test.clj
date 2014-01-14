(ns slack-hooks.service.tender-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.tender :as tender]
            [clojure.data.json :as json]))

(deftest convert-to-internal-url-test
  (let [discussion-url "http://site.com/discussions/12345"
        base-url "https://site.tenderapp.com/"]

    (testing "Returns internal URL"
      (is (= "https://site.tenderapp.com/discussions/12345"
             (tender/convert-to-internal-url discussion-url base-url))))

    (testing "Returns unmodified discussion URL without base URL"
      (is (= discussion-url
             (tender/convert-to-internal-url discussion-url nil))))))

(deftest message-from-request-body-test
  (let [request-body {:html_href "http://tender"
                      :author_name "Arthur Dent"
                      :body "Please send help."
                      :number 1
                      :internal true
                      :resolution nil
                      :system_message false
                      :discussion {:number 42
                                   :title "Halp!"
                                   :last_comment_id 24}}]

    (testing  "Returns a map from a given Tender webhook"
      (let [data (tender/message-from-request-body request-body)]
        (are [value property] (= value (data property))
             "http://tender" :href
             42 :number
             "Halp!" :title
             "Arthur Dent" :author
             24 :last-comment-id
             "Please send help." :body
             true :new-discussion?
             true :internal?
             false :resolved?
             false :system-message?)))

    (testing "Subsequent messages in a discussion"
      (let [request-body (assoc request-body :number 12)
            data (tender/message-from-request-body request-body)]
        (is (= false (data :new-discussion?)))))

    (testing "External messages"
      (let [request-body (assoc request-body :internal false)
            data (tender/message-from-request-body request-body)]
        (is (= false (data :internal?)))))

    (testing "Resolved discussion"
      (let [request-body (assoc request-body :resolution true)
            data (tender/message-from-request-body request-body)]
        (is (= true (data :resolved?)))))

    (testing "System messages"
      (let [request-body (assoc request-body :system_message true)
            data (tender/message-from-request-body request-body)]
        (is (= true (data :system-message?)))))))

(deftest tender-test
  (testing "Formatting a Tender webhook"
    (let [text (slurp "test/resources/tender.json")
          body (json/read-str text :key-fn keyword)]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: Title>\" was updated by user"
             (tender/tender-format {:body body}))))))
