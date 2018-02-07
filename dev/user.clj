(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clj-time.core :as time]
   [clj-time.coerce :as coerce]
   [clj-time.format :as time-format]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.walk :as walk]
   [com.stuartsierra.component :as component]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]

   [byte-streams :as bs]

   [manifold.bus :as mb]
   [manifold.deferred :as md]
   [manifold.stream :as ms]

   [aleph.http :as http]
   [taoensso.timbre :as log]

   [clojure.data.json :as json]
   [example.client :as client]
   [example.fake.client :as fake-client]
   [example.system :as system]
   [example.users :as users]
   [example.util :as util]))

(log/set-level! :debug)

(stest/instrument)

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

(defonce system nil)

(def url (str "http://localhost:" port))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (constantly (system/system config)))
  :init)

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (try
    (alter-var-root #'system component/start-system)
    :started
    (catch Exception ex
      (log/error (or (.getCause ex) ex) "Failed to start system.")
      :failed)))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop-system s))))
  :stopped)

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (Thread/sleep 250)
  (refresh :after `go))

(defn restart
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (go))

(defn test-article []
  {:source {:id nil, :name "Realclearpolitics.com"},
   :author "Joel Kotkin, City Journal",
   :title "Can the Trump Economy Trump Trump?",
   :description
   "Joel Kotkin, City Journal The president's economic policies are showing marked success, but he may not benefit politically unless he learns how to get out of his own way.",
   :url
   "https://www.realclearpolitics.com/2018/01/18/can_the_trump_economy_trump_trump_431798.html",
   :urlToImage nil})

(comment

  (get-articles "2018-01-24T08:30:00Z" "2018-01-24T11:50:00Z")
  (get-articles)
  (fake-client/add-tweet fake-url {:username "mike" :text "wat"})

  (fake-client/add-rand-apple-article fake-url)

  (fake-client/add-rand-apple-tweet fake-url)

  (def tf
    (future
      (while true
        (Thread/sleep (+ 250 (rand-int 1500)))
        (fake-client/add-rand-apple-tweet fake-url))))

  (future-cancel tf)

  (def af
    (future
      (while true
        (Thread/sleep (+ 1000 (rand-int 15000)))
        (fake-client/add-rand-apple-article fake-url))))

  (future-cancel af)
  )
