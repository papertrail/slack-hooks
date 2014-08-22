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
  [incident-type]
  (condp = incident-type
    "incident.trigger" "danger"
    "incident.resolve" "good"
    nil))

(defn format-user-objects
  [users]
  (->> users
       (map :object)
       (filter #(= "user" (:type %)))
       (map #(format "<%s|%s>" (:html_url %) (:name %)))
       distinct
       (string/join " & ")))

(defn incident-prefix
  [incident incident-type]
  (condp = incident-type
    "incident.trigger"       (let [service-name (-> incident :service :name)
                                   service-url  (-> incident :service :html_url)]
                               (if service-url
                                 (format "New incident from <%s|%s>" service-url service-name)
                                 (format "New incident from %s" service-name)))

    "incident.resolve"       (if-let [resolved-by (:resolved_by_user incident)]
                               (let [html-url    (:html_url resolved-by)
                                     name        (:name resolved-by)]
                                 (format "Resolved by <%s|%s>" html-url name))
                               (let [service-name (-> incident :service :name)
                                     service-url  (-> incident :service :html_url)]
                                 (if service-url
                                   (format "Resolved by <%s|%s>" service-url service-name)
                                   (format "Resolvedy by %s" service-name))))

    "incident.acknowledge"   (->> incident
                                  :acknowledgers
                                  format-user-objects
                                  (format "Acknowledged by %s"))

    "incident.delegate"      (->> incident
                                  :assigned_to
                                  format-user-objects
                                  (format "Assigned to %s"))

    "incident.assign"        (->> incident
                                  :assigned_to
                                  format-user-objects
                                  (format "Assigned to %s"))

    "incident.escalate"      (->> incident
                                  :assigned_to
                                  format-user-objects
                                  (format "Escalated to %s"))

    "incident.unacknowledge" (->> incident
                                  :assigned_to
                                  format-user-objects
                                  (format "Unacknowledged due to timeout and reassigned to %s"))

    (format "%s received" incident-type)))

(defn incident-title
  "Returns a title of the given Pagerduty alert."
  [incident incident-type]
  (let [incident-number (:incident_number incident)
        incident-url    (:html_url incident)
        prefix          (incident-prefix incident incident-type)]
    (format "<%s|#%s>: %s"
            incident-url
            incident-number
            prefix)))

(defn incident-description
  "Returns the descrption of the given Pagerduty alert."
  [incident]
  (let [summary-data (:trigger_summary_data incident)
        summary-vals (vals summary-data)]
    (or
      (:subject summary-data)
      (:description summary-data)
      (if (= 1 (count summary-vals))
        (first summary-vals)))))

(defn pagerduty-message->slack
  "Takes an individual message from a PagerDuty webhook and returns a map
  describing a message to send to Slack"
  [message]
  (let [incident      (-> message :data :incident)
        incident-type (:type message)]
    {:title       (incident-title incident incident-type)
     :description (incident-description incident)
     :color       (incident-color incident-type)}))

(defn pagerduty
  [request]
  (doseq [message (-> request :body :messages)
          :let [slack (pagerduty-message->slack message)]]
    (prn "message:" message)
    (slack/notify {:username    pagerduty-username
                   :icon_url    pagerduty-avatar
                   :attachments [{:pretext (:title slack)
                                  :text    (:description slack)
                                  :color   (:color slack)}]}))

  :submitted)
