(ns example.main
  (:require [example.system :as system]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:gen-class :main true))

(def port 8001)

(def tweet-url "https://stream.twitter.com/1.1/statuses/filter.json")

(def news-api-url "https://newsapi.org/v2")
(def news-api-key "58ef69549bf64f55a5dbdebefcdcf47f")

(def fake-url (str "http://localhost:" port "/fake"))
(def fake-tweet-url (str fake-url "/tweets/streaming"))

(def fake-news-api-url (str fake-url "/articles"))

(def config
  {:id "example-server"
   :port port
   :log-path "/tmp"
   :secret-key "secret"
   :user-manager-type "atomic"
   :users {"mike" "rocket"}
   :twitter-config {:url fake-tweet-url
                    :creds (edn/read-string (slurp "dev-resources/creds.edn"))
                    :params {:track "apple" :language "en"}}
   :news-api-config {:url fake-news-api-url
                     :api-key news-api-key
                     :period (* 60 60 1000)
                     :interval 3000
                     :retries 5}})

(defn start []
  (let [system (system/system config)]
    (log/info "Starting system.")
    (component/start-system system)
    system))

(defn -main
  [& args]
  (log/set-level! :debug)
  (log/debug "yo")
  (let [system (system/system config)]
    (log/info "Starting system.")
    (component/start-system system)
    (log/info "Waiting forever.")
    @(promise)))
