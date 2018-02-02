(ns example.news-api.daemon
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [manifold.time :as mt]
            [taoensso.timbre :as log]
            [example.twitter.auth :as auth]))


(defrecord NewsApiDaemon [url api-key params sink]
  component/Lifecycle
  (start [this]
    (let [source (ms/periodically 60000 )]


      ))
  (stop [this]
    (if source
      (do (log/info "Stopping Twitter daemon...")
          (ms/close! source)
          (log/info "Stopped.")
          (assoc this :source nil))
      (do (log/info (str "Twitter daemon already stopped."))
          this))))

(defn handy-daemon [url creds params sink]
  (map->TwitterDaemon {:url url :creds creds :params params :sink sink}))

(defn daemon [config]
  (let [{:keys [url creds params]} (:twitter-config config)]
    (component/using
     (map->TwitterDaemon {:url url
                          :creds (auth/creds creds)
                          :params params})
     {:sink :tweet-stream})))

(s/def :twitter/url string?)

(s/def :twitter/consumer-key string?)
(s/def :twitter/consumer-secret string?)
(s/def :twitter/user-token string?)
(s/def :twitter/user-token-secret string?)

(s/def :twitter/creds (s/keys :req-un [:twitter/consumer-key
                                       :twitter/consumer-secret
                                       :twitter/user-token
                                       :twitter/user-token-secret]))

(s/def :twitter/params map?)

(s/def :twitter/config (s/keys :req-un [:twitter/url
                                        :twitter/creds
                                        :twitter/params]))

(comment
  (def url "https://stream.twitter.com/1.1/statuses/filter.json")
  (def creds (auth/read-creds "dev-resources/creds.edn"))
  (def params {:track "puppy" :language "en"})

  (def response (filter-tweets url creds params))

  (def s (:body @response))

  s


  (def s (:body  x))

  item-1
  (def item-1 (bs/to-string @(ms/take! s)))
  (def item-2 (bs/to-string @(ms/take! s)))
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

  (ms/close! s)


  )
