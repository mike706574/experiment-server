(ns example.news-api.consumer
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.bus :as mb]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [manifold.time :as mt]
            [taoensso.timbre :as log]
            [example.news-api.repo :as repo]))

(defn process-article
  [repo bus article]
  (try
    (log/debug "Processing article:" (:title article))
    (if (repo/has? repo article)
      (log/debug "I already have it!")
      (let [tagged-article (repo/store! repo article)]
        (log/debug (str "New article:" tagged-article))
        (mb/publish! bus :all tagged-article)))
    (catch Exception ex
      (.printStackTrace ex))))

(defrecord NewsApiConsumer [repo bus stream]
  component/Lifecycle
  (start [this]
    (ms/consume (partial process-article repo bus) stream)
    this)
  (stop [this]
    this))

(defn consumer [config]
  (component/using
   (map->NewsApiConsumer {})
   {:bus :news-api-bus
    :repo :news-api-repo
    :stream :news-api-stream}))
