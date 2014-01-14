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

(deftest tender-test
  (testing "Formatting a Tender webhook"
    (let [text (slurp "test/resources/tender.json")
          body (json/read-str text :key-fn keyword)]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: Title>\" was updated by user"
             (tender/tender-format {:body body}))))))
