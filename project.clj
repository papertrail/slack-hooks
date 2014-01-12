(defproject slack-proxy "0.1.0-SNAPSHOT"
  :description "Relay webhooks to Slack webhooks"
  :url "https://github.com/lmarburger/slack-proxy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-jetty-adapter "1.1.6"]]
  :uberjar-name "slack-proxy-standalone.jar"
  :min-lein-version "2.0.0")
