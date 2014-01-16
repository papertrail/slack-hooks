(ns slack-hooks.service.travis-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.travis :as travis]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a Travis CI webhook"
    (let [text            (slurp "test/resources/travis-ci.json")
          request         {:params {:payload text}}
          formatted-text  (travis/travis-format request)]
      (is (= "[build] #1 (<1744327fb856|https://github.com/eric/north-american-bear/compare/48178e556ef8...1744327fb856>) by eric of <north-american-bear/master|https://github.com/eric/north-american-bear> failed in 15s — <https://travis-ci.org/eric/north-american-bear/builds/16906218>"
             formatted-text)))))
