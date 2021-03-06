(ns example.handler
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [example.api.handler :as api-handler]
            [example.streaming.handler :as streaming-handler]
            [example.fake.handler :as fake-handler]
            [example.util :as util]
            [ring.middleware.defaults :refer [wrap-defaults
                                              api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log]))

(defn not-found [req]
  {:status 404
   :headers {"content-type" "text/plain"}
   :body "Not found (root handler)."})

(defn wrap-logging
  [handler]
  (fn [{:keys [uri request-method] :as request}]
    (let [label (str (-> request-method name str/upper-case) " \"" uri "\"")]
      (try
        (log/debug label)
        (let [{:keys [status] :as response} (handler request)]
          (log/debug (str label " -> " status))
          (log/trace "Full response:\n" (util/pretty response))
          response)
        (catch Exception e
          (log/error e label)
          {:status 500})))))

(defn root-handler [deps]
  (let [handlers {:api (wrap-logging (api-handler/handler deps))
                  :fake (fake-handler/handler deps)
                  :streaming (wrap-logging (streaming-handler/handler deps))
                  :not-found not-found}]
    (fn [request]
      (let [uri (:uri request)
            handler-key (condp (util/flip str/starts-with?) uri
                          "/api" :api
                          "/fake" :fake
                          "/streaming" :streaming
                          :not-found)
            handler (get handlers handler-key)]
        (log/trace (str "Routing \"" uri "\" to " handler-key " handler."))
        (handler request)))))

(defprotocol HandlerFactory
  "Builds a request handler."
  (handler [this]))

(defrecord ExampleHandlerFactory [news-api-bus
                                  news-api-repo
                                  tweet-repo
                                  tweet-bus
                                  fake-tweet-bus
                                  conn-manager]
  HandlerFactory
  (handler [this]
    (-> this
        (root-handler)
        (wrap-params)
        (wrap-json-body {:keywords? true}))))

(defn factory
  [config]
  (component/using
   (map->ExampleHandlerFactory {})
   [:conn-manager :fake-tweet-bus :news-api-bus
    :news-api-repo :tweet-repo :tweet-bus]))
