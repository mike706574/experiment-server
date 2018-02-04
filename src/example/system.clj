(ns example.system
  (:require [example.authentication :as auth]
            [example.connection :as conn]
            [example.handler :as handler]
            [example.service :as service]
            [example.users :as users]
            [example.util :as util]

            [example.news-api.client :as news-api-client]
            [example.news-api.consumer :as news-api-consumer]
            [example.news-api.daemon :as news-api-daemon]
            [example.news-api.repo :as news-api-repo]

            [example.twitter.consumer :as tweet-consumer]
            [example.twitter.daemon :as tweet-daemon]
            [example.twitter.repo :as tweet-repo]

            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [manifold.bus :as mb]
            [manifold.stream :as ms]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defn configure-logging!
  [{:keys [id log-path] :as config}]
  (let [log-file (str log-path "/" id "-" (util/uuid))]
    (log/merge-config!
     {:appenders {:spit (appenders/spit-appender
                         {:fname log-file})}})))

(s/def :service/id string?)
(s/def :service/port integer?)
(s/def :service/log-path string?)
(s/def :service/user-manager-type #{"atomic"})
(s/def :service/users (s/map-of :service/username :service/password))

(s/def :service/twitter-config :twitter/config)

(s/def :service/config (s/keys :req-un [:service/id
                                        :service/port
                                        :service/log-path
                                        :service/user-manager-type
                                        :service/twitter-config]
                               :opt-un [:service/users]))

(defn process-tweet
  [bucket tweet]
  (swap! bucket conj tweet))

(defn ^:private build
  [config]
  (log/info (str "Building " (:id config) "."))
  (configure-logging! config)
  {:authenticator (auth/authenticator config)

   :twitter-daemon (tweet-daemon/daemon config)
   :tweet-consumer (tweet-consumer/consumer config)
   :tweet-stream (ms/stream)
   :tweet-repo (tweet-repo/atomic config)
   :tweet-bus (mb/event-bus)

   :news-api-params (atom {:language "en"
                           :q "apple"
                           :sources "the-wall-street-journal,reuters,financial-times,the-economist"})
   :news-api-client (news-api-client/client config)
   :news-api-daemon (news-api-daemon/daemon config)
   :news-api-consumer (news-api-consumer/consumer config)
   :news-api-stream (ms/stream)
   :news-api-repo (news-api-repo/atomic config)
   :news-api-bus (mb/event-bus)

   :conn-manager (conn/manager config)

   :fake-tweet-bus (mb/event-bus)

   :handler-factory (handler/factory config)

   :app (service/aleph-service config)})

(defn system
  [config]
  (if-let [validation-failure (s/explain-data :service/config config)]
    (do (log/error (str "Invalid configuration:\n"
                        (util/pretty config)
                        "Validation failure:\n"
                        (util/pretty validation-failure)))
        (throw (ex-info "Invalid configuration." {:config config
                                                  :validation-failure validation-failure})))
    (build config)))

(s/fdef system
  :args (s/cat :config :service/config)
  :ret map?)
