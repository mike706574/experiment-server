(ns example.streaming.handler
  (:require [aleph.http :as http]
            [clojure.data.json :as json]
            [compojure.core :as compojure :refer [GET POST]]
            [compojure.route :as route]
            [example.connection :as conn]
            [manifold.bus :as mb]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [manifold.time :as mt]
            [taoensso.timbre :as log]))

(defn non-websocket-response
  []
  {:status 400
   :headers {"content-type" "text/plain"}
   :body "Expected a websocket request."})

(defn tweet-ws
  [{:keys [tweet-bus conn-manager] :as deps} req]
  (md/let-flow [conn (md/catch
                         (http/websocket-connection req)
                         (constantly nil))]
    (if-not conn
      (non-websocket-response)
      (let [conn-id (conn/add! conn-manager :menu conn)
            conn-label (str "[ws-conn-" conn-id "] ")]
        (log/debug (str conn-label "Tweet websocket connection established."))
        (try
          (ms/connect-via
           (mb/subscribe tweet-bus :all)
           #(ms/put! conn (json/write-str %))
           conn)
          {:status 101}
          (catch Exception e
            (log/error e (str conn-label "Exception thrown while setting up connection."))
            {:status 500}))))))

(defn handler
  [deps]
  (compojure/routes
   (GET "/streaming/tweets" [] (partial tweet-ws deps))
   (route/not-found "No such page.")))