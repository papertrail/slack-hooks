(ns slack-hooks.service.pagerduty
  (:require [slack-hooks.slack :as slack]
            [clojure.string :as string]))

(def pagerduty-username
  (or
    (System/getenv "PAGERDUTY_USERNAME")
    "pagerduty"))

(def pagerduty-avatar
  (System/getenv "PAGERDUTY_AVATAR"))

(defn incident-color
  [payload]
  (condp = (:type payload)
    "incident.trigger" "danger"
    "incident.resolve" "good"
    nil))

(defn incident-description
  [payload]
  (let [summary-data (-> payload :data :incident :trigger_summary_data)]
    (or
      (:subject summary-data)
      (:description summary-data))))

(defn format-user-objects
  [users]
  (->> users
       (map :object)
       (filter #(= "user" (:type %)))
       (map #(format "<%s|%s>" (:html_url %) (:name %)))
       distinct
       (string/join " & ")))

(defn incident-prefix
  [payload]
  (condp = (:type payload)
    "incident.trigger"       (let [service-name (-> payload :data :incident :service :name)
                                   service-url  (-> payload :data :incident :service :html_url)]
                               (format "New incident from <%s|%s>" service-url service-name))

    "incident.acknowledge"   (let [acknowledgers (-> payload :data :incident :acknowledgers)
                                   names         (format-user-objects acknowledgers)]
                               (format "Acknowledged by %s" names))

    "incident.delegate"      (let [assigned-to (-> payload :data :incident :assigned_to)
                                   names       (format-user-objects assigned-to)]
                               (format "Assigned to %s" names))

    "incident.resolve"       (let [resolved-by (-> payload :data :incident :resolved_by_user)
                                   html-url     (:html_url resolved-by)
                                   name         (:name resolved-by)]
                               (format "Resolved by <%s|%s>" html-url name))

    "incident.assign"        (let [assigned-to (-> payload :data :incident :assigned_to)
                                   names       (format-user-objects assigned-to)]
                               (format "Assigned to %s" names))

    "incident.escalate"      (let [assigned-to (-> payload :data :incident :assigned_to)
                                   names       (format-user-objects assigned-to)]
                               (format "Escalated to %s" names))

    "incident.unacknowledge" (let [assigned-to (-> payload :data :incident :assigned_to)
                                   names       (format-user-objects assigned-to)]
                               (format "Unacknowledged due to timeout and reassigned to %s" names))

    (format "%s received" (:type payload))))

(defn formatted-message
  "Returns a description of the given Pagerduty alert."
  [payload]
  (let [incident        (-> payload :data :incident)
        incident-number (:incident_number incident)
        incident-url    (:html_url incident)
        prefix          (incident-prefix payload)
        description     (incident-description payload)]
    (format "<%s|#%s>: %s: %s"
            incident-url
            incident-number
            prefix
            description)))


(defn pagerduty
  [request]
  (doseq [message (-> request :body :messages)]
    (prn "message:" message)
    (slack/notify {:username    pagerduty-username
                   :icon_url    pagerduty-avatar
                   :attachments [{:text  (formatted-message message)
                                  :color (incident-color message)}]})))
