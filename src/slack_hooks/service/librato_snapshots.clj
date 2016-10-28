(ns slack-hooks.service.librato-snapshots
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [slack-hooks.slack :as slack]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-http.client :as client]))

(def librato-username
   (or
     (System/getenv "LIBRATO_USERNAME")
     "Librato Alert Snapshots"))

(def librato-avatar
    (System/getenv "LIBRATO_AVATAR"))

(def librato-slack-url
   (System/getenv "LIBRATO_SLACK_URL"))


;  - If chart with the Alert Name doesn't exist, create it.
;  - Remove all metrics inside the chart.
;  - Load metrics that were sent in the webhook. DONE
;  - Snapshot that Chart
;  - Send the Slack message

(def librato-api-user
  (System/getenv "LIBRATO_API_USER"))

(def librato-api-token
  (System/getenv "LIBRATO_API_TOKEN"))

(def chart-duration
  (or
    (System/getenv "LIBRATO_CHART_DURATION") 
    "3600"))

(def librato-space-id
    (System/getenv "LIBRATO_SPACE_ID"))

(defn librato-api-url
  [path]
    (str/join ["https://metrics-api.librato.com" path])
  )

(def librato-request-options 
  {
    :basic-auth [librato-api-user librato-api-token]; "767ba449fa0ef4d508ca698d3f452db1f52b22e97d26c50798b0021e48d54c0e"]
    :content-type :json
    :accept :json
    :throw-entire-message? true
  })

(defn snapshot-image
  "Requests the Snapshot. Tries 5 times to get the image URL. Waits 2 seconds between each try.
  Returns nil if it can't get the URL."
   [poll-url times]
  (let [
       request (client/get (librato-api-url poll-url) librato-request-options)
       image (get (json/read-str (:body request)) "image_href")
       ]
       (if (> times 5) 
         nil 
         (if (not (nil? image)) 
           image
           (or (Thread/sleep 2000) (snapshot-image poll-url (+ times 1))
          )))
      )
  )

(defn snapshot-chart
  "Snapshots the chart"
   [chart-id chart-duration]
    (let [
          form-data {
                     :subject 
                      {
                        :chart {:id chart-id, :source nil, :type "line"}
                        :duration chart-duration
                      }
                    }
          request (client/post (librato-api-url "/v1/snapshots") (merge librato-request-options {:body (json/write-str form-data)}))
          ]
          ; Poll this for image_href
          (get (json/read-str (:body request)) "href")
  ))

(defn create-chart
  "Creates the Chart in the Librato Space. Returns the created chart ID."
  [chart-name chart-data]
   (let [ form-data {
                    :name chart-name,
                    :type "line",
                    :streams chart-data
                  }
        request (client/post (format (librato-api-url  "/v1/spaces/%s/charts") librato-space-id )
                   (merge librato-request-options  {:body (json/write-str form-data)}))
    ]
     (last (str/split (get (:headers request) "location") #"/"))
  )
)
(defn update-chart 
  "Updates the chart in Librato. Returns the chart ID."
  [chart-id chart-data]
   (let [ form-data {
                    :type "line",
                    :streams chart-data
                  }
        request (client/put (format (librato-api-url  "/v1/spaces/%s/charts/%s") librato-space-id chart-id)
                   (merge librato-request-options  {:body (json/write-str form-data)}))
    ]
     chart-id
  )
)

(defn chart-exists? 
  "Returns the Chart ID if a chart exists. nil otherwise"
  [chart-name]
  (let [
        request (client/get (format (librato-api-url  "/v1/spaces/%s/charts") librato-space-id ) librato-request-options)
        ]
      (loop [charts (json/read-str (:body request))] 
        (if (nil? charts) nil
          (if 
            (= chart-name (get (first charts) "name")) 
            (get (first charts) "id") 
            (recur (next charts))
          )
        )
       )
      )
   )


(defn get-metrics
  [metrics]
  "Gets the metrics from the violations."
  (distinct (flatten (map (fn [s]
            (let [source (first s)]
              (map (fn [m] {:metric (:metric m), :source source}  )  (first (rest s))) 
            )
            ) metrics)))
  )

(defn thresholds-for-metric [conditions condition_violated]
  (loop [condition conditions]
    (if (nil? condition) nil
      (if
        (= condition_violated (:id (first condition)))
         (format "%s %s for %s seconds" (:type (first condition)) (:threshold (first condition)) (:duration (first condition)))
         (recur (next condition))
        )
      )
    )
  )

(defn metric-message [conditions violations]
  
  (let [
        metric-condition (distinct (flatten (map 
                       (fn [s] 
                            (map 
                              (fn [m] (format "`%s` went %s" (:metric m) (thresholds-for-metric conditions (:condition_violated m)))  )
                              (first (rest s))) 
            ) violations)))
        ]
    metric-condition
    )
  )

(defn offending-sources [violations]
  (distinct (flatten (map (fn [s]
                          (let [source (first s)]
                            (map (fn [m] (format "- %s, reaching %.2f" (name source) (:value m)) )  (first (rest s)))
                          )) violations)))
  )

(defn librato [request]
  "Receives the Librato Webhook. Sends a slack message."
  (let [payload         (-> request :params :payload)
        data            (json/read-str payload :key-fn keyword)
        alert-name      (:name (:alert data))
        conditions      (:conditions data)
        violations      (:violations data)
        metrics         (get-metrics violations)
        chart-id        (chart-exists? alert-name)
        chart           (if (not (nil? chart-id)) (update-chart chart-id metrics) (create-chart alert-name metrics))
        snapshot        (snapshot-image (snapshot-chart chart chart-duration) 1)
        text_1          (first (metric-message conditions violations))
        sources         (offending-sources violations)
        space-link      (librato-api-url (str/join ["/spaces" librato-space-id] ))
        slack-message   {
          :title      (str alert-name "has fired!")
          :title_link (str "https://metrics.librato.com/alerts#" (:id (:alert data)))
          :image_url  snapshot
          :text       (str/join "\n" [(str text_1 " on:") (str/join sources) (str "<" space-link "|Alerting Chart>")])
          :fallback   (str alert-name "has fired!")
        }
        ]

      (slack/notify {:slack-url   librato-slack-url
                     :username    librato-username
                     :icon_url    librato-avatar
                     :attachments [slack-message]
                   }

        )
    )
  )
