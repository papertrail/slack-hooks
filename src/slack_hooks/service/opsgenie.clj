(ns slack-hooks.service.opsgenie
  (:require [slack-hooks.slack :as slack]))

(def opsgenie-username
  (or
    (System/getenv "OPSGENIE_USERNAME")
    "opsgenie"))

(def opsgenie-avatar
  (System/getenv "OPSGENIE_AVATAR"))

(def description-formats-by-action
  {"Create"          "opened by %s"
   "Close"           "closed by %s"
   "Delete"          "deleted by %s"
   "Acknowledge"     "acknowledged by %s"
   "AddRecipient"    "recipient added %3$s by %1$s"
   "AssignOwnership" "assigned to %2$s by %1$s"
   "TakeOwnership"   "owned by %s"})

(defn formatted-message
  "Returns a description of the given OpsGenie alert."
  [request]
  (let [payload (:body request)
        {:keys [action alert]} payload
        {:keys [message username tinyId owner recipient]} alert
        fmt         (description-formats-by-action action "updated by %s with action %4$s")
        href        (str "http://opsg.in/i/" tinyId)
        description (format fmt username owner recipient action)]
      (format "<%s|\"%s\"> %s"
              href
              message
              description)))

(defn opsgenie
  "Accepts an HTTP request from an OpsGenie webhook and reports the details to
  a Slack webhook."
  [request]
  (let [options {:username opsgenie-username
                 :icon_url opsgenie-avatar
                 :text (formatted-message request)}]
    (prn request)
    (prn options)
    (slack/notify options)))
