(ns slack-hooks.hooks-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.github :as github]
            [slack-hooks.service.tender :as tender]
            [slack-hooks.service.travis :as travis]
            [clojure.data.json :as json]))

(deftest travis-test
  (testing "Formatting Travis CI webhook"
    (let [payload (slurp "test/resources/travis-ci.json")]
      (is (= "[build] #1 (<1744327fb856dc9ce1ed23c82486d4a612456ee6|https://github.com/eric/north-american-bear/compare/48178e556ef8...1744327fb856>) by eric of north-american-bear/master failed — <https://travis-ci.org/eric/north-american-bear/builds/16906218>"
             (travis/travis-format {:params {:payload payload}}))))))

(deftest tender-test
  (testing "Formatting a Tender webhook"
    (let [text (slurp "test/resources/tender.json")
          body (json/read-str text :key-fn keyword)]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: Title>\" was updated by user"
             (tender/formatted-message {:body body}))))))
