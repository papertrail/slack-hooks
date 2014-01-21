(ns slack-hooks.service.opsgenie-test
  (:use clojure.test)
  (:require [slack-hooks.service.opsgenie :as opsgenie]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a new OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-open.json")
                        (json/read-str :key-fn keyword))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> opened by System (via API)"
             formatted))))

  (testing "Formatting an acknowledged OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-close.json")
                        (json/read-str :key-fn keyword)
                        (assoc :action "Acknowledge"))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> acknowledged by arthur@dent.com"
             formatted))))

  (testing "Formatting an assigned OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-close.json")
                        (json/read-str :key-fn keyword)
                        (assoc :action "AssignOwnership")
                        (assoc-in [:alert :owner] "ford@prefect.com"))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> assigned to ford@prefect.com by arthur@dent.com"
             formatted))))

  (testing "Formatting an add recipient OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-close.json")
                        (json/read-str :key-fn keyword)
                        (assoc :action "AddRecipient")
                        (assoc-in [:alert :recipient] "ford@prefect.com"))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> recipient added ford@prefect.com by arthur@dent.com"
             formatted))))

  (testing "Formatting a closed OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-close.json")
                        (json/read-str :key-fn keyword))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> closed by arthur@dent.com"
             formatted))))

  (testing "Foramtting a deleted OpsGenie alert"
    (let [payload   (-> (slurp "test/resources/opsgenie-close.json")
                        (json/read-str :key-fn keyword)
                        (assoc :action "Delete"))
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "<http://opsg.in/42|\"Database on fire\"> deleted by arthur@dent.com"
             formatted))))

  (testing "Formatting an unexpected message"
    (let [payload   {:message "Database on fire"}
          formatted (opsgenie/formatted-message {:body payload})]
      (is (= "{:message \"Database on fire\"}"
             formatted))))
  )
