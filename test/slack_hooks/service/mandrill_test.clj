(ns slack-hooks.service.mandrill-test
  (:use clojure.test)
  (:require [slack-hooks.service.mandrill :as mandrill]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (with-redefs [mandrill/customer-search-base-url "https://site.com/customers?q="]
    (testing "Formatting a rejected Mandrill webhook"
      (let [text            (slurp "test/resources/mandrill-reject.json")
            request         {:params {:mandrill_events text}}
            formatted-text  (mandrill/formatted-message request)]
        (is (= ["Email to <https://site.com/customers?q=arthur@dent.com|arthur@dent.com> from support@site.com was rejected: \"Database on fire\""]
               formatted-text))))

    (testing "Formatting multiple rejected Mandrill webhooks"
      (let [text            (slurp "test/resources/mandrill-multiple.json")
            request         {:params {:mandrill_events text}}
            formatted-text  (mandrill/formatted-message request)]
        (is (= ["Email to <https://site.com/customers?q=arthur@dent.com|arthur@dent.com> from support@site.com was rejected: \"Database on fire\""
                "Email to <https://site.com/customers?q=ford@prefect.com|ford@prefect.com> from support@site.com was rejected: \"App under water\""]
               formatted-text))))))
