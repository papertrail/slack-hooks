(ns slack-hooks.slack-test
  (:require [clojure.test :refer :all]
            [slack-hooks.slack :as slack]))

(deftest escape-message-test
  (let [data {:text "This is <a> test."}]
    (testing "Escapes HTML characters"
      (is (= {:text "This is &lt;a&gt; test."}
             (slack/escape-message data))))))
