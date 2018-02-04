(ns example.news-api.daemon-test
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

(def news-api-config {:url fake-url
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
  (def daemon news-api-config)

  (def system (component/start-system {:news-api-daemon (daemon config)
                                       :news-api-client (client/client config)
                                       :news-api-params (atom params)}))

  (component/stop-system system)

  (def client (client/client config))

  (client/get-articles client (assoc params :from "2018-01-29"))

  (def sink (ms/stream))
  (ms/consume #(println "Consumed article:" %) sink)
  (def run (partial domain/run client sink (* 60 60 1000) params))

  (def ctrl (ms/stream))
  (def deferred (domain/start-daemon 3000 5 ctrl run))
  (.getName (Thread/currentThread))

  (ms/close! ctrl)

  (ms/consume #(println "Consumed article:" %) stream)

  ctrl
  (ms/close! ctrl)

  @(md/future (Thread/sleep 4000))

  (fake-client/add-rand-apple-article "http://localhost:8001/fake")

  (get-articles client params)

  (def poll (partial process-articles client stream params))

  (poll)

  (def x (every 3000 poll))

  x

  (future-cancel x)








  (get-articles fake-url api-key {:language "en"
                                  :q "apple"
                                  :sources "the-wall-street-journal,reuters,financial-times,the-economist"

                                  :from "2018-01-29"})


  (def apple-articles (->> (get-all-articles url api-key {:language "en"
                                           :q "apple"
                                           :sources "the-wall-street-journal,reuters,financial-times,the-economist"

                                           :from "2018-01-01"})
            (map #(dissoc % :publishedAt))))

  (spit "dev-resources/apple-articles-1.json" (json/write-str apple-articles))

  (get-articles fake-url api-key {:page 1})

  (def d (every fake-url api-key {:language "en"
                                  :q "apple"
                                  :sources "the-wall-street-journal,reuters,financial-times,the-economist"
                                  :from "2018-01-29"} 5000))

  (future-cancel d)



  (get-all-articles fake-url api-key {})

  (get-articles fake-url)
  (def fut (every fake-url api-key {} 5000))
  (future-cancel fut)
  fut
  (test-article)

  fut)
