(ns example.twitter.daemon
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [taoensso.timbre :as log]
            [example.twitter.auth :as auth]))

(defn filter-tweets
  [url creds query-params]
  (let [auth-header (auth/auth-header creds :post url query-params)]
    (http/post url
               {:query-params query-params
                :headers {"Authorization" auth-header}
                :pool (http/connection-pool {:connection-options {:raw-stream? true}})})))

(defn connect
  [url creds query-params]
  (md/chain (let [auth-header (auth/auth-header creds :post url query-params)]
              (http/post url
                         {:query-params query-params
                          :headers {"Authorization" auth-header}
                          :pool (http/connection-pool {:connection-options {:raw-stream? true}})}))
    (fn [response] (log/spy :trace response))
    :body
    #(ms/map bs/to-string %)
    #(ms/map (fn [chunk] (log/trace (str "Chunk: |" chunk "|")) chunk) %)
    #(ms/filter (complement str/blank?) %)))

(defn error-desc [ex]
  (if (instance? java.net.ConnectException ex)
    "Connection refused"
    (when-let [status (:status (ex-data ex))]
      (case status
        416 "Rate limited"
        500 "Server error"
        (str "Failed with status " status)))))

(defn process-chunk
  [buffer sink chunk]
  (try
    (.append buffer chunk)
    (log/trace (str "Appending chunk: |"  chunk "|"))
    (when (str/ends-with? chunk "\r\n")
      (let [raw-tweet (str buffer)
            {:keys [user text id] :as full-tweet} (json/read-str raw-tweet :key-fn keyword)
            tweet {:id id :username (:screen_name user) :text text}]
        (.setLength buffer 0)
        (log/trace (str "Sending tweet: " tweet))
        (ms/put! sink tweet)))
    (catch Exception ex
      (log/error ex "Error processing chunk."))))

(defn start-daemon [url creds query-params control sink]
  (let [buffer (StringBuffer.)
        process-chunk (partial process-chunk buffer sink)
        stream (atom nil)]
    (md/loop [attempts 1]
      (-> (connect url creds query-params)
          (md/chain
            (fn [source]
              (log/info "Connection established.")
              (let [disconnect (md/deferred)]
                (ms/on-drained source #(md/success! disconnect ::disconnect))
                (ms/connect source sink {:upstream? true :downstream? false})
                (reset! stream source)
                (md/alt (ms/take! control ::drained) disconnect)))
            (fn [msg]
              (case msg
                ::drained (do (log/info "Drained - terminating.")
                              (ms/close! @stream)
                              (println stream))
                ::disconnect (do (log/info "Disconnected - attempting to reconnect.")
                                 (md/recur 1))
                (log/error (str "Unexpected message: " msg)))))
          (md/catch Exception
              (fn [ex]
                (if-let [error-desc (error-desc ex)]
                  (let [wait (* 1000 (math/expt 2 attempts))]
                    (log/debug (str error-desc " - reconnecting in " wait " milliseconds (attempt #" (inc attempts) ")."))
                    (md/chain (ms/try-take! control ::drained wait ::timeout)
                      (fn [msg]
                        (case msg
                          ::timeout (md/recur (inc attempts))
                          ::drained (log/debug "Drained - terminating.")))))
                  (log/error ex "Unexpected exception."))))))))

(defn chunk-stream [tweet-stream]
  (let [stream (ms/stream)]
    (ms/on-drained stream #(log/debug "Chunk stream drained."))
    (ms/consume (partial process-chunk (StringBuffer.) tweet-stream) stream)
    stream))

(defrecord TwitterDaemon [url creds params sink source control deferred]
  component/Lifecycle
  (start [this]
    (if source
      (do (log/info (str "Twitter daemon already started."))
          this)
      (let [source (chunk-stream sink)
            control (ms/stream)]
        (log/info "Starting Twitter daemon.")
        (let [deferred (start-daemon url creds params control sink)]
          (assoc this :source source :control control :deferred deferred)))))
  (stop [this]
    (if source
      (do (log/info "Stopping Twitter daemon...")
          (ms/close! source)
          (ms/close! control)
          @deferred
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
