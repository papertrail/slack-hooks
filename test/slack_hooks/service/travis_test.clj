(ns slack-hooks.service.travis-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.travis :as travis]
            [clojure.data.json :as json]))

(deftest status-avatar
  (with-redefs [travis/travis-avatar-failed "failed.png"
                travis/travis-avatar-passed "passed.png"]
    (testing "Passing tests"
      (let [text    (-> (slurp "test/resources/travis-ci.json")
                        (json/read-str :key-fn keyword)
                        (assoc :result 0)
                        (assoc :status 0)
                        json/write-str)
            request {:params {:payload text}}
            avatar  (travis/status-avatar request)]
      (is (= "passed.png" avatar))))

    (testing "Failing tests"
      (let [text    (-> (slurp "test/resources/travis-ci.json")
                        (json/read-str :key-fn keyword)
                        (assoc :result 1)
                        (assoc :status 1)
                        json/write-str)
            request {:params {:payload text}}
            avatar  (travis/status-avatar request)]
      (is (= "failed.png" avatar))))))

(deftest formatted-message-test
  (testing "Formatting a Travis CI webhook"
    (let [text            (slurp "test/resources/travis-ci.json")
          request         {:params {:payload text}}
          formatted-text  (travis/travis-format request)]
      (is (= "[build] #1 (<https://github.com/eric/north-american-bear/compare/48178e556ef8...1744327fb856|1744327f>) by eric of <https://github.com/eric/north-american-bear/tree/master|north-american-bear/master> failed in 15s — <https://travis-ci.org/eric/north-american-bear/builds/16906218>"
             formatted-text))))


  (testing "Formatting a Travis CI pull request webhook"
    (let [text            (slurp "test/resources/travis-ci-pull.json")
          request         {:params {:payload text}}
          formatted-text  (travis/travis-format request)]
      (is (= "[build] #10 (<https://github.com/eric/north-american-bear/pull/1|517eba97>) by eric of <https://github.com/eric/north-american-bear/tree/master|north-american-bear/pull/1> passed in 17s — <https://travis-ci.org/eric/north-american-bear/builds/17048460>"
             formatted-text)))))

(deftest pretty-duration-test
  (testing "zero seconds"
    (is (= "0s" (travis/pretty-duration 0))))

  (testing "twenty seconds"
    (is (= "20s" (travis/pretty-duration 20))))

  (testing "one hundred seconds"
    (is (= "1m40s" (travis/pretty-duration 100)))))
