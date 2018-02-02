(ns example.twitter.daemon-test
  (:require [byte-streams :as bs]
            [clojure.test :refer [deftest is]]
            [example.twitter.auth :as auth]
            [example.twitter.daemon :as domain]
            [manifold.stream :as ms]
            [manifold.deferred :as md]))

(def url "https://stream.twitter.com/1.1/statuses/filter.json")
(def fake-url "http://localhost:8001/fake/tweets")
(def creds (auth/read-creds "dev-resources/creds.edn"))
(def params {:track "puppy" :language "en"})

(comment


  (def s (:body @(domain/filter-tweets fake-url creds params)))

  s

  (bs/to-string @(ms/take! s))

  item-1
  (def item-1 (bs/to-string @(ms/take! s)))

  item-1
  (def item-2 (bs/to-string @(ms/take! s)))

  item-2
  (def item-3 (bs/to-string @(ms/take! s)))
  (def item-4 (bs/to-string @(ms/take! s)))


  (ms/close! s)


  (defn tweet-stream [bucket]
    (let [stream (ms/stream)]
      (ms/on-drained stream #(log/debug "Tweet stream drained..."))
      (ms/consume (partial process-tweet bucket) stream)
      stream))


  (def bucket (atom []))
  (def ts (tweet-stream bucket))
  (def td (twitter-daemon url creds params ts))

  td
  (def td (component/start td))
  (def td (component/stop td))
  td

  (ms/close! tacotown)




  (def ts (tweet-stream bucket))
  (def cs (chunk-stream ts))

  (daemon turl creds {:track "trump" :language "en"} cs)

  (connect turl creds {:track "trump" :language "en"} cs)
    ;; (ms/consume (partial process-tweet (StringBuffer.) bucket) sink)
  (ms/close! cs)


  (def s (ms/stream))

  (def wat (every-so-often s 10000 10000 #(do (println "running")
                                              (ms/put! s "tick"))))

  @(ms/take! s)
  (ms/put! s "wat")

  (ms/close! s))
