(ns slack-hooks.service.librato-test
  (:use clojure.test)
  (:require [slack-hooks.service.librato-snapshots :as librato]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a Librato webhook"
    (with-redefs [
                  librato/chart-exists?   (fn [chart-name] 123)
                  librato/update-chart   (fn [chart-name data] 123)
                  librato/snapshot-image (fn [chart times] "http://example.com/snapshot.jpg")
                  librato/snapshot-chart (fn [url duration] "http://example.com/")
                  librato/librato-space-id 1234
                  ]

      (let [text (slurp "test/resources/librato-alert.json")
            request (json/read-str text :key-fn keyword)
            data (first (:attachments (librato/slack-message request)))
            space-link "https://metrics.librato.com/s/spaces/1234"
            chart-link "https://metrics.librato.com/s/spaces/1234/explore/123"
            alert-link "https://metrics.librato.com/alerts#12345"
            snapshot-link "http://example.com/snapshot.jpg"
            ]
        (is (= "Here's the chart for the-test-alert" (:title data)))
        (is (= space-link (:title_link data)))
        (is (= (str "<" chart-link "|See alerting metrics>\n<" alert-link "|Go to alert definition>" ) (:text data)))
        (is (= snapshot-link (:image_url data)))
        (is (= "the-test-alert has triggered!" (:fallback data)))

        ))))
