(ns slack-hooks.service.scout-test
  (:use clojure.test)
  (:require [slack-hooks.service.scout :as scout]))

(deftest formatted-message-test
  (testing "Formatting a Scout webhook"
    (let [text (slurp "test/resources/scout-alert.json")
          request {:params {:payload text}}
          data (scout/scout-message request)]
      (is (= "[Alert start] Load Average on Blade" (:title data)))
      (is (= "https://scoutapp.com/a/999999" (:title_link data)))
      (is (= "Load Average - Last minute met or exceeded 3.00 , increasing to 3.50 at 01:06AM" (:text data)))
      (is (= "[Alert start] Load Average on Blade - https://scoutapp.com/a/999999" (:fallback data)))
      (is (= "danger" (:color data)))
             )))
