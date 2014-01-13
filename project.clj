(defproject slack-proxy "1.0.0-SNAPSHOT"
  :description "Relay webhooks to Slack webhooks"
  :url "https://github.com/lmarburger/slack-proxy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring/ring-json "0.2.0"]
                 [clj-http "0.7.8"]]
  :dev-dependencies [[ring/ring-devel "0.2.0"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler slack-proxy.core/app}
  :main slack-proxy.core
  :uberjar-name "slack-proxy-standalone.jar"
  :min-lein-version "2.0.0")
