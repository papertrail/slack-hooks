(ns slack-hooks.slack-test
  (:use clojure.test)
  (:require [slack-hooks.slack :as slack]))

(deftest escape-test
  (testing "Escapes HTML characters"
    (is (= "This is &lt;a&gt; test."
           (slack/escape "This is <a> test.")))))
