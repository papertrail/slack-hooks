(ns slack-hooks.service.github
  (:require [ring.util.response :refer [response]]))

(defn github [request]
  (response (str request)))