(ns example.news-api.daemon
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [com.stuartsierra.component :as component]
            [example.news-api.client :as client]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [taoensso.timbre :as log]))

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))
(def parse (partial time-format/parse time-format))
(def unparse (partial time-format/unparse time-format))

(defn run [client sink period params]
  (let [to (time/now)
        from (time/ago (time/seconds (/ period 1000)))
        params-with-range (assoc params :from (unparse from) :to (unparse to))
        articles (client/get-articles client params)]
    (log/trace (str "Got " (count articles) " articles."))
    (doseq [article articles] (ms/put! sink article))))

(defn start-daemon [interval retries control f]
  (md/loop [attempts 1]
    (-> (md/future (f))
        (md/chain
            (fn [_] (ms/try-take! control ::drained interval ::timeout))
            (fn [msg]
              (case msg
                ::timeout (md/recur 1)
                ::drained (log/debug "Drained - terminating.")
                (log/error (str "Unexpected message: " msg)))))
        (md/catch Exception
            (fn [ex]
              (log/error ex (str "Failed to retrieve News API articles (attempt " attempts " of " retries ")."))
              (Thread/sleep interval)
              ;; TODO: What to do here?
              (if (> attempts retries)
                (log/error "Retry attempts exhausted - terminating.")
                (md/recur (inc attempts))))))))

(defrecord NewsApiDaemon [client period interval retries sink params control deferred]
  component/Lifecycle
  (start [this]
    (if deferred
      (do (log/info (str "News API daemon already started."))
          this)
      (let [run (partial run client sink period @params)
            control (ms/stream)]
        (log/info "Starting News API daemon.")
        (let [deferred (start-daemon interval retries control run)]
          (assoc this :control control :deferred deferred)))))
  (stop [this]
    (if deferred
      (do (log/info "Stopping News API daemon...")
          (ms/close! control)
          @deferred
          (log/info "Stopped.")
          (assoc this :control nil :deferred nil))
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
