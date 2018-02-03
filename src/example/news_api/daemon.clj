(ns example.news-api.daemon
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [aleph.http :as http]
            [byte-streams :as bs]
            [example.util :as util]
            [example.fake.client :as fake-client]
            [example.twitter.auth :as auth]
            [example.news-api.client :as client]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [manifold.time :as mt]

            [taoensso.timbre :as log]
            ))

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))
(def parse (partial time-format/parse time-format))
(def unparse (partial time-format/unparse time-format))

(defn process-articles [client sink period params]
  (let [to (time/now)
        from (time/ago (time/seconds (/ period 1000)))
        params-with-range (assoc params :from (unparse from) :to (unparse to))
        articles (client/get-articles client params)]
    (log/debug (str "Got " (count articles) " articles."))
    (doseq [article articles] (ms/put! sink article))))

(defn start-daemon [interval retries control f]
  (md/loop [attempts 0]
    (let [attempts (try
                     (f)
                     0
                     (catch Exception ex
                       (log/error ex (str "Failed to poll for News API articles (attempt " attempts " of " retries ").") )
                       (inc attempts)))]
      ;; TODO: It would probably be preferable to keep running.
      (if (> attempts retries)
        (log/error "Retry attempts exhausted. Terminating.")
        (-> (ms/try-take! control ::drained interval ::timeout)
            (md/chain (fn [msg]
                        (case msg
                          ::timeout (d/recur attempts)
                          ::drained (log/debug "Drained.")))))))))

(defrecord NewsApiDaemon [client period interval retries sink params control future]
  component/Lifecycle
  (start [this]
    (if future
      (do (log/info (str "News API daemon already started."))
          this)
      (let [poll (partial process-articles client sink period @params)
            control (ms/stream)]
        (log/info "Starting News API daemon.")
        (let [future (start-daemon interval retries control poll)]
          (assoc this :control control :future future)))))
  (stop [this]
    (if future
      (do (log/info "Stopping News API daemon...")
          (ms/close! control)
          @future
          (log/info "Stopped.")
          (assoc this :control nil :future nil))
      (do (log/info (str "News API daemon already stopped."))
          this))))

(defn daemon [config]
  (let [{:keys [period interval retries]} (:news-api-config config)]
    (component/using
     (map->NewsApiDaemon {:period period
                          :interval interval
                          :retries retries})
     {:client :news-api-client
      :sink :news-api-stream
      :params :news-api-params})))

(def api-key "58ef69549bf64f55a5dbdebefcdcf47f")
(def url "https://newsapi.org/v2")

(def fake-url "http://localhost:8001/fake/articles")

(defn test-article []
  {:source {:id nil, :name "Realclearpolitics.com"},
   :author "Joel Kotkin, City Journal",
   :title "Can the Trump Economy Trump Trump?",
   :description
   "Joel Kotkin, City Journal The president's economic policies are showing marked success, but he may not benefit politically unless he learns how to get out of his own way.",
   :url
   "https://www.realclearpolitics.com/2018/01/18/can_the_trump_economy_trump_trump_431798.html",
   :urlToImage nil,
   :publishedAt (unparse (time/now))})

(def params {:language "en"
             :q "apple"
             :sources "the-wall-street-journal,reuters,financial-times,the-economist"})

(def news-api-config {:url url
                      :api-key api-key
                      :period (* 60 60 1000)
                      :interval 3000
                      :retries 5})

(def config {:news-api-config news-api-config})

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
  (def poll (partial process-articles client sink (* 60 60 1000) params))
  (poll)
  (poll)
  (def ctrl (ms/stream))
  (def deferred (daemon 3000 5 ctrl poll))


  (ms/consume #(println "Consumed article:" %) stream)

  ctrl
  (ms/close! ctrl)

  @(md/future (Thread/sleep 4000))

  (fake-client/add-article "http://localhost:8001/fake" (test-article))

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
