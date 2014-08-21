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
                               (format "New incident from <%s|%s>" service-url service-name))

    "incident.resolve"       (let [resolved-by (:resolved_by_user incident)
                                   html-url    (:html_url resolved-by)
                                   name        (:name resolved-by)]
                               (format "Resolved by <%s|%s>" html-url name))

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
  [payload]
  )

(defn incident-title
  "Returns a title of the given Pagerduty alert."
  [payload]
  (let [incident        (-> payload :data :incident)
        incident-number (:incident_number incident)
        incident-url    (:html_url incident)
        incident-type   (:type payload)
        prefix          (incident-prefix incident incident-type)]
    (format "<%s|#%s>: %s"
            incident-url
            incident-number
            prefix)))

(defn incident-description
  "Returns the descrption of the given Pagerduty alert."
  [payload]
  (let [summary-data (-> payload :data :incident :trigger_summary_data)
        summary-vals (vals summary-data)]
    (or
      (:subject summary-data)
      (:description summary-data)
      (if (= 1 (count summary-vals))
        (first summary-vals)))))

(defn pagerduty
  [request]
  (doseq [message (-> request :body :messages)]
    (prn "message:" message)
    (slack/notify {:username    pagerduty-username
                   :icon_url    pagerduty-avatar
                   :attachments [{:pretext (incident-title message)
                                  :text    (incident-description message)
                                  :color   (incident-color message)}]}))

  :submitted)
