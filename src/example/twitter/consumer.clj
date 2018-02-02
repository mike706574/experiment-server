(ns example.twitter.consumer
  (:require [com.stuartsierra.component :as component]
            [example.twitter.repo :as repo]
            [manifold.bus :as mb]
            [manifold.stream :as ms]))

(defn process-tweet
  [repo bus tweet]
  (println "Tweet: " tweet)
  (repo/store! repo tweet)
  (mb/publish! bus :all tweet))

(defrecord TweetConsumer [repo bus stream]
  component/Lifecycle
  (start [this]
    (ms/consume (partial process-tweet repo bus) stream)
    this)
  (stop [this]
    this))

(defn consumer [config]
  (component/using
   (map->TweetConsumer {})
   {:repo :tweet-repo
    :bus :tweet-bus
    :stream :tweet-stream}))
