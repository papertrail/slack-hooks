(ns slack-proxy.hooks-test
  (:require [clojure.test :refer :all]
            [slack-proxy.hooks :refer :all]))

(deftest travis-test
  (testing "Formatting Travis CI webhook"
    (let [payload (slurp "test/resources/travis-ci.json")]
      (is (= "[build] #1 (<1744327fb856dc9ce1ed23c82486d4a612456ee6|https://github.com/eric/north-american-bear/compare/48178e556ef8...1744327fb856>) by eric of north-american-bear/master failed — <https://travis-ci.org/eric/north-american-bear/builds/16906218>"
             (travis-format {:params {:payload payload}}))))))

;(deftest tender-test
;  (testing "Formatting a Tender webhook"
;    (let [data (slurp "test/resources/tender.json")]
;      (is (= "" data)))))