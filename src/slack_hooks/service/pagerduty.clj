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
  (case (:type payload)
    "incident.trigger" "danger"
    "incident.resolve" "good")

(defn incident-description
  [payload]
  (let [summary-data (-> payload :data :incident :trigger_summary_data)]
    (or
      (:subject summary-data)
      (:description summary-data))))


(defn incident-prefix
  [payload]
  (case (:type payload)
    "incident.trigger"       (let [service-name (-> payload :data :incident :service :name)
                                   service-url  (-> payload :data :incident :service :html_url)]
                               (format "New incident from <%s|%s>" service-url service-name))

    "incident.acknowledge"   (let [acknowledgers (-> payload :data :incident :acknowledgers)
                                   names         (->> acknowledgers
                                                      (map :object)
                                                      (filter #(= "user" (:type %)))
                                                      (map #(format "<%s|%s>" (:html_url %) (:name %)))
                                                      (string/join " & "))]
                               (format "Acknowledged by %s" names))
    "incident.delegate"      (let [assigned-to (-> payload :data :incident :assigned_to)
                                   names       (->> assigned-to
                                                    (map :object)
                                                    (filter #(= "user" (:type %)))
                                                    (map #(format "<%s|%s>" (:html_url %) (:name %)))
                                                    (string/join " & "))]
                               (format "Assigned to %s" names))
    "incident.resolve"       (let [resolved-by (-> payload :data :incident :resolved_by_user)
                                   html-url     (:html_url resolved-by)
                                   name         (:name resolved-by)]
                               (format "Resolved by <%s|%s>" html-url name))
    "incident.unacknowledge" "Unacknowledged due to timeout")
    (format "%s received"    (:type payload))))

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
    (slack/notify {:username    pagerduty-username
                   :icon_url    pagerduty-avatar
                   :attachments [{:text  (formatted-message request)
                                  :color (incident-color message)}]})))
