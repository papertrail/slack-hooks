(ns slack-hooks.service.github
  (:use ring.util.response
        [clojure.string :only [lower-case]])
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]))

(defn github [request]
  (response (str request)))