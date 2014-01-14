(ns slack-hooks.service.tender-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.tender :as tender]
            [clojure.data.json :as json]))


(deftest tender-test
  (testing "URL replacement"
    (with-redefs [tender/tender-base-url "https://site.tenderapp.com/"]
                 (is (= "https://site.tenderapp.com/path"
                        (tender/tender-internal-url "http://site.com/path")))))

  (testing "Formatting a Tender webhook"
    (let [text (slurp "test/resources/tender.json")
          body (json/read-str text :key-fn keyword)]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: Title>\" was updated by user"
             (tender/tender-format {:body body}))))))