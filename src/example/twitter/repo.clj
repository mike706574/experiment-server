(ns example.twitter.repo)

(defprotocol TweetRepo
  (store! [this tweet])
  (all [this]))

(defrecord AtomicTweetRepo [tweets]
  TweetRepo
  (store! [this tweet]
    (swap! tweets conj tweet))
  (all [this]
    @tweets))

(defn atomic [config]
  (map->AtomicTweetRepo {:tweets (atom [])}))
