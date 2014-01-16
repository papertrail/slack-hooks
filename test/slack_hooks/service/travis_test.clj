(ns slack-hooks.service.travis-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.travis :as travis]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a Travis CI webhook"
    (let [text            (slurp "test/resources/travis-ci.json")
          request         {:params {:payload text}}
          formatted-text  (travis/travis-format request)]
      (is (= "[build] #1 (<https://github.com/eric/north-american-bear/compare/48178e556ef8...1744327fb856|1744327fb856>) by eric of <https://github.com/eric/north-american-bear|north-american-bear/master> failed in 15s — <https://travis-ci.org/eric/north-american-bear/builds/16906218>"
             formatted-text)))))
