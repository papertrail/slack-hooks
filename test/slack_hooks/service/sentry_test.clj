(ns slack-hooks.service.sentry-test
  (:use clojure.test)
  (:require [slack-hooks.service.sentry :as sentry]
            [clojure.data.json :as json]))

(deftest formatted-message-test
  (testing "Formatting a Sentry webhook"
    (let [text (slurp "test/resources/sentry.json")
          json (json/read-str text  :key-fn keyword)
          data (sentry/sentry->slack json)]
      (is (= "*project: projectname: <https://beta.getsentry.com/project/project/group/123/|ZeroDivisionError: divided by 0>*"
             (:title data)))
      (is (= "bin/raven:36: in `<main>'\n```    if !dsn\n      puts \"Usage: raven test <dsn>\"\n    else\n      Raven::CLI::test(dsn)\n    end\n  else\n    puts parser\n```"
             (:description data))))))