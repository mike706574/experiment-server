(ns example.twitter.daemon-test
  (:require [byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [example.twitter.daemon :as domain]
            [manifold.stream :as ms]
            [manifold.deferred :as md]))

(def url "https://stream.twitter.com/1.1/statuses/filter.json")
(def fake-url "http://localhost:8001/fake/tweets/streaming")
(def creds (edn/read-string (slurp "dev-resources/creds.edn")))
(def params {:track "puppy" :language "en"})

(comment
  (def s (:body @(domain/filter-tweets fake-url creds params)))
  (ms/close! s)

  (def control (ms/stream))
  (def sink (ms/stream))
  (ms/consume #(println "Hi!") sink)
  (def d (domain/start-daemon fake-url creds {} control sink))

  (ms/close! control)
  )
