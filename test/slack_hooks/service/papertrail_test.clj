(ns slack-hooks.service.papertrail-test
  (:use clojure.test)
  (:require [slack-hooks.service.papertrail :as papertrail]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a Papertrail search webhook"
    (let [text (slurp "test/resources/papertrail.json")
          request {:form-params {"payload" text}}
          formatted-text (papertrail/formatted-message request)]
      (is (= "Search <https://papertrailapp.com/searches/4242|\"Kernel (unexpected)\"> found 10 matches"
             formatted-text)))))
