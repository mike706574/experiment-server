(ns example.twitter.daemon
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [manifold.time :as mt]
            [taoensso.timbre :as log]
            [example.twitter.auth :as auth]))

(defn filter-tweets
  [url creds query-params]
  (let [auth-header (auth/auth-header creds :post url query-params)]
    (http/post url
               {:query-params query-params
                :headers {"Authorization" auth-header}
                :pool (http/connection-pool {:connection-options {:raw-stream? true}})})))

(defn connect-streams [source sink]
  (log/info "Connection established.")
  (let [disc (md/deferred)]
    (ms/connect source sink {:upstream? true :downstream? false})
    (ms/on-drained source #(md/success! disc :disconnect))
    disc))

(defn connect
  [url creds query-params sink]
  (md/chain (let [auth-header (auth/auth-header creds :post url query-params)]
              (http/post url
                         {:query-params query-params
                          :headers {"Authorization" auth-header}
                          :pool (http/connection-pool {:connection-options {:raw-stream? true}})}))
    (fn [response] (log/spy :debug response))
    :body
    #(ms/map bs/to-string %)
    ;;    #(ms/map (fn [chunk] (println (str "Chunk: |" chunk "|")) chunk) %)
    #(ms/filter (complement str/blank?) %)
    #(connect-streams % sink)))

;; TODO: md/future
(defn start-daemon [url creds query-params control sink]
  (md/loop [attempts 0]
    (try
      (if (ms/closed? sink)
        (log/info "Stream closed - terminating.")
        (let [thing @(md/alt (ms/take! control ::drained)
                             (connect url creds query-params sink))]
          (case thing
            ::stop (log/info "Stopped.")
            ::drained (log/info "Drained.")
            (do (log/info "Disconnected.")
                (md/recur 0)))))
      (catch java.net.ConnectException ex
        (let [wait (* attempts 1000)]
          (log/debug (str "Connection refused - reconnecting in " wait " milliseconds (attempt #" (inc attempts) ")."))
          (Thread/sleep wait)
          (md/recur (inc attempts))))
      (catch InterruptedException e
        (log/debug "Terminating."))
      (catch Exception ex
        (if-let [{:keys [status] :as data} (ex-data ex)]
          (let [error-desc (case status
                             416 "Rate limited"
                             500 "Server error"
                             (str "Failed with status " status))
                wait (* 1000 (math/expt 2 attempts))]
            (log/debug (str error-desc " - reconnecting in " wait " milliseconds (attempt #" (inc attempts) ")."))
            (Thread/sleep wait)
            (md/recur (inc attempts)))
          (log/error "Unexpected exception." ex))))))

(defn process-chunk
  [buffer tweet-stream chunk]
  (try
    (.append buffer chunk)
    (log/debug (str "Appending chunk: |"  chunk "|"))
    (when (str/ends-with? chunk "\r\n")
      (let [raw-tweet (str buffer)
            {:keys [user text id] :as full-tweet} (json/read-str raw-tweet :key-fn keyword)
            tweet {:id id :username (:screen_name user) :text text}]
        (.setLength buffer 0)
        ;;        (log/debug (str "Sending tweet: " tweet))
        (ms/put! tweet-stream tweet)))
    (catch Exception ex
      (log/error ex "Error processing chunk."))))

(defn chunk-stream [tweet-stream]
  (let [stream (ms/stream)]
    (ms/on-drained stream #(log/debug "Chunk stream drained."))
    (ms/consume (partial process-chunk (StringBuffer.) tweet-stream) stream)
    stream))

(defrecord TwitterDaemon [url creds params sink source future]
  component/Lifecycle
  (start [this]
    (if source
      (do (log/info (str "Twitter daemon already started."))
          this)
      (let [source (chunk-stream sink)]
        (log/info "Starting Twitter daemon.")
        (let [future (start-daemon url creds params source)]
          (assoc this :source source :future future)))))
  (stop [this]
    (if source
      (do (log/info "Stopping Twitter daemon...")
          (ms/close! source)
          @(future)
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
                          :creds creds
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
