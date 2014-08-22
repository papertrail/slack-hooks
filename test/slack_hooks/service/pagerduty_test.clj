(ns slack-hooks.service.pagerduty-test
  (:use clojure.test)
  (:require [slack-hooks.service.pagerduty :as pagerduty]
            [clojure.data.json :as json]))

(deftest incident-title-test
  (testing "Formatting a Pagerduty open"
    (let [payload (-> (slurp "test/resources/pagerduty-open.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          incident-type (:type payload)
          formatted (pagerduty/incident-title incident incident-type)]
      (is (= "<https://your.pagerduty.com/incidents/P3OK5H|#48>: New incident from <https://your.pagerduty.com/services/P0SVHU|Checker>"
             formatted))))

  (testing "Formatting a Pagerduty acknowledge"
    (let [payload (-> (slurp "test/resources/pagerduty-acknowledge.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          incident-type (:type payload)
          formatted (pagerduty/incident-title incident incident-type)]
      (is (= "<https://your.pagerduty.com/incidents/P3OK5H|#48>: Acknowledged by <https://your.pagerduty.com/users/PNAO4I|User>"
             formatted))))

  (testing "Formatting a Pagerduty delegate"
    (let [payload (-> (slurp "test/resources/pagerduty-delegate.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          incident-type (:type payload)
          formatted (pagerduty/incident-title incident incident-type)]
      (is (= "<https://your.pagerduty.com/incidents/PBK84G|#47>: Assigned to <https://your.pagerduty.com/users/PNAO4I|User>"
             formatted))))

  (testing "Formatting a Pagerduty resolve"
    (let [payload (-> (slurp "test/resources/pagerduty-resolve.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          incident-type (:type payload)
          formatted (pagerduty/incident-title incident incident-type)]
      (is (= "<https://your.pagerduty.com/incidents/P3OK5H|#48>: Resolved by <https://your.pagerduty.com/users/PNAO4I|User>"
             formatted)))))

(deftest incident-description-test
  (testing "Formatting a Pagerduty open"
    (let [payload (-> (slurp "test/resources/pagerduty-open.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          formatted (pagerduty/incident-description incident)]
      (is (= "Alert text"
             formatted))))

  (testing "Formatting a Pagerduty acknowledge"
    (let [payload (-> (slurp "test/resources/pagerduty-acknowledge.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          formatted (pagerduty/incident-description incident)]
      (is (= "Something happened"
             formatted))))

  (testing "Formatting a Pagerduty delegate"
    (let [payload (-> (slurp "test/resources/pagerduty-delegate.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          formatted (pagerduty/incident-description incident)]
      (is (= "testing a webhook"
             formatted))))

  (testing "Formatting a Pagerduty resolve"
    (let [payload (-> (slurp "test/resources/pagerduty-resolve.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident (-> payload :data :incident)
          formatted (pagerduty/incident-description incident)]
      (is (= "an alert"
             formatted)))))

(deftest incident-color-test
  (testing "Color of a Pagerduty open"
    (let [payload (-> (slurp "test/resources/pagerduty-open.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident-type (:type payload)
          color (pagerduty/incident-color incident-type)]
      (is (= "danger" color))))

  (testing "Color of a Pagerduty acknowledge"
    (let [payload (-> (slurp "test/resources/pagerduty-acknowledge.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident-type (:type payload)
          color (pagerduty/incident-color incident-type)]
      (is (= nil color))))

  (testing "Color of a Pagerduty delegate"
    (let [payload (-> (slurp "test/resources/pagerduty-delegate.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident-type (:type payload)
          color (pagerduty/incident-color incident-type)]
      (is (= nil color))))

  (testing "Color of a Pagerduty resolve"
    (let [payload (-> (slurp "test/resources/pagerduty-resolve.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          incident-type (:type payload)
          color (pagerduty/incident-color incident-type)]
      (is (= "good" color)))))

(deftest pagerduty-message->slack-test
  (testing "Slack message from webhook"
    (let [payload (-> (slurp "test/resources/pagerduty-open.json")
                      (json/read-str :key-fn keyword)
                      :messages
                      first)
          slack (pagerduty/pagerduty-message->slack payload)]
      (is (contains? slack :title))
      (is (contains? slack :description))
      (is (contains? slack :color)))))
