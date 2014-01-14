(ns slack-proxy.hooks-test
  (:require [clojure.test :refer :all]
            [slack-proxy.hooks :refer :all]))

(deftest travis-test
  (testing "Formatting Travis CI webhook"
    (is (= "[build] #null (<null|null>) by null of null/null  — <null>"
           (travis-format {:params {:payload "{}"}})))))
