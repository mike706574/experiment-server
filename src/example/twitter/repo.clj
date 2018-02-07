(ns example.twitter.repo
  (:require [clojure.spec.alpha :as s]))

(defprotocol TweetRepo
  (-store! [this tweet])
  (-page [this number])
  (-all [this]))

(defrecord AtomicTweetRepo [tweets]
  TweetRepo
  (-store! [this tweet]
    (swap! tweets conj tweet))
  (-page [this number]
    (take 10 (drop (* 10 (dec number)) @tweets)))
  (-all [this]
    @tweets))

(defn atomic [config]
  (map->AtomicTweetRepo {:tweets (atom [])}))

(defn store! [repo tweet] (-store! repo tweet))
(defn page [repo number] (-page repo number))
(defn all [repo] (-all repo))

(s/fdef page
  :args (s/cat :repo any? :number integer?))
