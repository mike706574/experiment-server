(ns example.twitter.daemon-test
  (:require [byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [example.fake.client :as fake-client]
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


  (def s (ms/stream))

  (ms/on-drained s #(println "hello"))

  (ms/close! nil)



  (def source (domain/connect url creds params sink))

  source

  @(ms/take! @source)

  (ms/close! @source)

  source

  (def control (ms/stream))
  (def sink (ms/stream))
  (ms/consume (fn [tweet] (println "Got tweet:" tweet))  sink)
  (def d (domain/start-daemon fake-url creds params control sink))

  (fake-client/add-tweet "http://localhost:8001/fake" {:username "mike" :text "whoa"})
  (ms/close! control)

  (def sink (ms/stream))
  (def d (domain/connect url creds params sink))
  (ms/take! @d)

  (ms/close! @d)
  (ms/close! sink)


  (def a (ms/stream))
  (def b (ms/stream))
  (def s (ms/connect a b))
  (def s (ms/connect-via a #(ms/put! b (inc %)) b))
  @s

  @s






)
