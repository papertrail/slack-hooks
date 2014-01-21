(ns slack-hooks.service.opsgenie
  (:require [slack-hooks.slack :as slack]))

(def opsgenie-username
  (or
    (System/getenv "OPSGENIE_USERNAME")
    "opsgenie"))

(def opsgenie-avatar
  (System/getenv "OPSGENIE_AVATAR"))

(defn description-from-action
  [action username owner recipient]
  (if-let [fmt (condp = action
                 "Create"          "opened by %s"
                 "Close"           "closed by %s"
                 "Delete"          "deleted by %s"
                 "Acknowledge"     "acknowledged by %s"
                 "AddRecipient"    "recipient added %3$s by %1$s"
                 "AssignOwnership" "assigned to %2$s by %1$s"
                 nil)]
    (format fmt username owner recipient)))

(defn message-from-payload
  [payload]
  (let [{:keys [action alert]} payload
        {:keys [message username tinyId owner recipient]} alert]
    (if-let [description (description-from-action
                           action username owner recipient)]
      {:description description
       :message  message
       :username username
       :href     (str "http://opsg.in/" tinyId)})))

(defn formatted-message
  "Returns a description of the given OpsGenie alert."
  [request]
  (let [payload (:body request)]
    (if-let [message (message-from-payload payload)]
      (format "<%s|\"%s\"> %s"
              (:href message)
              (:message message)
              (:description message))
      (str payload))))

(defn opsgenie
  "Accepts an HTTP request from an OpsGenie webhook and reports the details to
  a Slack webhook."
  [request]
  (let [options {:username opsgenie-username
                 :text (formatted-message request)}]
    (prn request)
    (prn options)
    (slack/notify options)))
