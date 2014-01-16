(ns slack-hooks.service.tender-test
  (:require [clojure.test :refer :all]
            [slack-hooks.service.tender :as tender]
            [clojure.data.json :as json]))

(deftest swap-base-url-test
  (let [url "http://site.com/discussions/42"
        base-url "https://site.tenderapp.com/"]

      (testing "Swaps hostname and protocol"
        (is (= "https://site.tenderapp.com/discussions/42"
               (tender/swap-base-url url base-url))))

      (testing "Returns original url without base url"
        (is (= url
               (tender/swap-base-url url nil))))))

(deftest internal-message-link-test
  (with-redefs [tender/tender-base-url "https://site.tenderapp.com/"]
    (let [message {:href "http://site.com/discussions/42"
                   :last-comment-id 24}]
      (testing "Returns internal link for a message"
        (is (= "https://site.tenderapp.com/discussions/42#comment_24"
               (tender/internal-message-link message)))))))

(deftest message-action
  (let [message {:new-discussion? false
                 :resolved?       false
                 :internal?       false
                 :system-message? false}]
    (testing "Public update"
      (is (= "updated" (tender/message-action message))))

    (testing "Opened"
      (let [message (assoc message :new-discussion? true)]
        (is (= "opened" (tender/message-action message)))))

    (testing "Resolved"
      (let [message (assoc message :resolved? true)]
        (is (= "resolved" (tender/message-action message)))))

    (testing "Internal update"
      (let [message (assoc message :internal? true)]
        (is (= "updated (internal)" (tender/message-action message)))))))

(deftest message-from-request-test
  (let [request {:body {:html_href "http://tender"
                        :author_name "Arthur Dent"
                        :body "Please send help."
                        :number 1
                        :internal true
                        :resolution nil
                        :system_message false
                        :discussion {:number 42
                                     :title "Halp!"
                                     :last_comment_id 24}}}]

    (testing  "Returns a map from a given Tender webhook"
      (let [data (tender/message-from-request request)]
        (are [value property] (= value (data property))
             "http://tender" :href
             42 :number
             "Halp!" :title
             "Arthur Dent" :author
             24 :last-comment-id
             "Please send help." :body
             true :new-discussion?
             true :internal?
             false :resolved?
             false :system-message?)))

    (testing "Subsequent messages in a discussion"
      (let [request (assoc-in request [:body :number] 12)
            data (tender/message-from-request request)]
        (is (= false (data :new-discussion?)))))

    (testing "External messages"
      (let [request (assoc-in request [:body :internal] false)
            data (tender/message-from-request request)]
        (is (= false (data :internal?)))))

    (testing "Resolved discussion"
      (let [request (assoc-in request [:body :resolution] true)
            data (tender/message-from-request request)]
        (is (= true (data :resolved?)))))

    (testing "System messages"
      (let [request (assoc-in request [:body :system_message] true)
            data (tender/message-from-request request)]
        (is (= true (data :system-message?)))))))

(deftest formatted-message-test
  (testing "Formatting a normal Tender update"
    (let [text           (slurp "test/resources/tender.json")
          body           (json/read-str text :key-fn keyword)
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was updated by user"
             formatted-text))))

  (testing "Formatting a Tender update of a re-open"
    (let [text           (slurp "test/resources/tender.json")
          body           (-> (json/read-str text :key-fn keyword)
                             (assoc :internal true)
                             (assoc :system_message true)
                             (assoc :body "The discussion has been re-opened."))
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was re-opened by user"
             formatted-text))))


  (testing "Formatting a Tender update of a merge"
    (let [text           (slurp "test/resources/tender.json")
          body           (-> (json/read-str text :key-fn keyword)
                             (assoc :internal true)
                             (assoc :system_message true)
                             (assoc :body "Discussion was merged with \"too-8810\"."))
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was merged with \"too-8810\" by user"
             formatted-text))))

  (testing "Formatting a Tender update of a queue add"
    (let [text           (slurp "test/resources/tender.json")
          body           (-> (json/read-str text :key-fn keyword)
                             (assoc :internal true)
                             (assoc :system_message true)
                             (assoc :body "Discussion was added to Requests queue"))
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was added to Requests queue by user"
             formatted-text))))

  (testing "Formatting a Tender update of an added watcher"
    (let [text           (slurp "test/resources/tender.json")
          body           (-> (json/read-str text :key-fn keyword)
                             (assoc :internal true)
                             (assoc :system_message true)
                             (assoc :body "user@domain.com has been added as a watcher."))
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was updated by user: user@domain.com has been added as a watcher."
             formatted-text))))


  (testing "Formatting a Tender update of discussion to private"
    (let [text           (slurp "test/resources/tender.json")
          body           (-> (json/read-str text :key-fn keyword)
                             (assoc :internal true)
                             (assoc :system_message true)
                             (assoc :body "The discussion is now private."))
          formatted-text (tender/formatted-message {:body body})]
      (is (= "[tender] #9539 \"<http://help.app.com/discussions/email/9539#comment_31093891|Re: -&gt; Title>\" was updated by user: The discussion is now private."
             formatted-text))))

  )
