(ns example.news-api.client-test
  (:require [byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [example.news-api.client :as client]
            [example.news-api.daemon :as domain]
            [example.fake.client :as fake-client]
            [manifold.stream :as ms]
            [manifold.deferred :as md]))

(def api-key "58ef69549bf64f55a5dbdebefcdcf47f")
(def url "https://newsapi.org/v2")

(def fake-url "http://localhost:8001/fake/articles")

(def news-api-config {:url url
                      :api-key api-key
                      :period (* 60 60 1000)
                      :interval 3000
                      :retries 5})

(def config {:news-api-config news-api-config})

(def params
  {:language "en"
   :q "apple"
   :sources "the-wall-street-journal,reuters,financial-times,the-economist"})

(comment
  (def client (client/client config))

  (client/get-articles client (assoc params :from "2018-02-02"))

  )
