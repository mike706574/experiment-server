(ns example.twitter.consumer
  (:require [clj-time.coerce :as time-coerce]
            [clj-time.format :as time-format]
            [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [example.twitter.repo :as repo]
            [manifold.bus :as mb]
            [manifold.stream :as ms]
            [taoensso.timbre :as log]))

(def a-time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(defn build [{:keys [user id text timestamp_ms]}]

  {:id (str id)
   :author (:screen_name user)
   :text text
   :published-at(time-format/unparse a-time-format (time-coerce/to-date timestamp_ms))})

(defn process
  [repo bus text]
  (let [data (json/read-str text :key-fn keyword)
        tweet (build data)]
    (repo/store! repo tweet)
    (mb/publish! bus :all tweet)
    (log/trace (str "New tweet: " (:id tweet)))))

(defrecord TweetConsumer [repo bus stream]
  component/Lifecycle
  (start [this]
    (ms/consume (partial process repo bus) stream)
    this)
  (stop [this]
    this))

(defn consumer [config]
  (component/using
   (map->TweetConsumer {})
   {:repo :tweet-repo
    :bus :tweet-bus
    :stream :tweet-stream}))
