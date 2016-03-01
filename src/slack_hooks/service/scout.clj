(ns slack-hooks.service.scout
  (:require [clojure.data.json :as json]
            [slack-hooks.slack :as slack]))

(def scout-username
   (or
     (System/getenv "SCOUT_USERNAME")
     "Scout"))

(def scout-avatar
    (System/getenv "SCOUT_AVATAR"))

(def scout-slack-url
   (System/getenv "SCOUT_SLACK_URL"))

(defn alert-color [alert]
  (case alert
    "start"  "danger"
    "oneoff" "warning"
    "end"    "good"
    "warning"
    ))

(defn scout-message [request]
  (let [payload         (-> request :params :payload)
        data            (json/read-str payload :key-fn keyword)

        server_name     (:server_name data)
        server_hostname (:server_hostname data)

        alert_status    (:lifecycle data) ; can be [start|end|oneoff]  -- "oneoff" is for alerts generated internally by plugins
        alert_id        (:id data)
        alert_message   (:title data)
        alert_trigger   (:trigger data)
        alert_time      (:time data)
        alert_url       (:url data)
        alert_name      (:plugin_name data) ; Plugin that's alerting
        sparkline_url   (:sparkline_url data)

        ]
        (merge {
          :title (str (if (= alert_status "end") "Resolved: " "" ) (format "%s on %s" alert_name server_name))
          :title_link alert_url
          :text (format "%s - %s" alert_name alert_message)
          :fallback (format "[Alert %s] %s on %s - %s" alert_status alert_name server_name alert_url)
          :color (alert-color alert_status)
        }
        (if (not= alert_status "end") {:image_url sparkline_url} nil))
        ))

(defn scout [request]
  (let [scout-message (scout-message request)]
      (slack/notify {:slack-url   scout-slack-url
                     :username    scout-username
                     :icon_url    scout-avatar
                     :attachments [scout-message]
                   }

        )
    )
  )
