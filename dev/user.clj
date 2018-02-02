(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.walk :as walk]
   [com.stuartsierra.component :as component]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]

   [byte-streams :as bs]

   [manifold.bus :as mb]
   [manifold.deferred :as md]
   [manifold.stream :as ms]

   [aleph.http :as http]
   [taoensso.timbre :as log]

   [clojure.data.json :as json]
   [example.client :as client]
   [example.system :as system]
   [example.users :as users]
   [example.util :as util]))

(log/set-level! :debug)

(stest/instrument)

(def port 8001)

(def tweet-url "https://stream.twitter.com/1.1/statuses/filter.json")

(def fake-tweet-url (str "http://localhost:" port "/fake/tweets/streaming"))

(def config
  {:id "example-server"
   :port port
   :log-path "/tmp"
   :secret-key "secret"
   :user-manager-type "atomic"
   :users {"mike" "rocket"}
   :twitter-config {:url fake-tweet-url
                    :creds (edn/read-string (slurp "dev-resources/creds.edn"))
                    :params {:track "puppy" :language "en"}}})

(defonce system nil)

(def url (str "http://localhost:" port))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (constantly (system/system config)))
  :init)

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (try
    (alter-var-root #'system component/start-system)
    :started
    (catch Exception ex
      (log/error (or (.getCause ex) ex) "Failed to start system.")
      :failed)))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop-system s))))
  :stopped)

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (Thread/sleep 250)
  (refresh :after `go))

(defn restart
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (go))

(def url "http://localhost:8001/streaming/numbers")

;; @(http/get url)
(defn stream-numbers [msg-sink event-sink]
  (let [{:keys [status body] :as response} @(http/get url {:query-params {:length 5}
                                                           :throw-exceptions false
                                                           :read-timeout 30000})]
    (println "Response:" response)
    (if (= status 200)
      (let [body response
            msg-source (->> body
                            (ms/map bs/to-string)
                            (ms/filter (complement str/blank?)))]
        (ms/connect msg-source msg-sink {:downstream? false
                                         :upstream? true})
        {:ok true :msg-source msg-source})
      {:ok false :response response})))


(defn stream-numbers-to-failboat [sink]
  (md/chain
      (http/get "http://localhost:8001/streaming/numbers"
                {:query-params {:count 100}
                 :throw-exceptions false
                 :pool (http/connection-pool {:connection-options {:raw-stream? true}})})
    (fn [{:keys [status body] :as response}]
      (if (= status 200)
        (do (log/debug "Connection established.")
            (let [source (->> body
                              (ms/map bs/to-string)
                              (ms/filter (complement str/blank?)))]
              (ms/connect source sink {:upstream? true :downstream? false})
              true))
        (do (log/debug "Connection rejected." response)
            false)))))

(defn connect-to [sink source]
  (log/info "Connection established.")
  (let [disc (md/deferred)]
    (ms/connect source sink {:upstream? true :downstream? false})
    (ms/on-drained source #(md/success! disc :disconnect))
    disc))

(defn stream-numbers-to [sink]
  (md/chain
      (http/get "http://localhost:8001/streaming/numbers"
                {:query-params {:count 5}
                 :pool (http/connection-pool {:connection-options {:raw-stream? true}})})
    :body
    #(ms/map (comp str/trim bs/to-string) %)
    #(ms/filter (complement str/blank?) %)
    #(connect-to sink %)))


(defn streaming-daemon [sink]
  (md/loop [attempt 0]
    (try
      (let [disconnect @(stream-numbers-to sink)]
        (if (ms/closed? sink)
          (log/info "Connection closed - terminating.")
          (do (log/info "Disconnected - reconnecting.")
              (md/recur 0))))
      (catch Exception ex
        (if-let [{:keys [status] :as data} (ex-data ex)]
          (let [error-desc (case status
                       416 "Rate limited"
                       500 "Server error"
                       (str "Failed with status " status))
                wait (* 1000 (math/expt 2 attempt))]
            (log/debug (str error-desc " - reconnecting in " wait " milliseconds (attempt #" (inc attempt) ")."))
            (Thread/sleep wait)
            (md/recur (inc attempt)))
          (log/error "Unexpected exception." ex))))))

(defn process-number [counter number]
  (swap! counter inc)
  (log/info (str "Received number: " number " (#" @counter ").")))

(def test-article {:source {:id nil, :name "Nationalreview.com"},
                   :author "Joel Kotkin",
                   :title "Can the Trump Economy Trump Trump?",
                   :description
                   "Editor’s Note: The following piece originally appeared in City Journal. It is reprinted here with permission.\n\n\n\tPresident Trump’s critics find it hard to give him credit for anything, especially given his extraordinary boastfulness. Yet Trump’s economic poli…",
                   :url
                   "http://www.nationalreview.com/article/455707/donald-trump-economic-growth-will-republicans-benefit-politically",
                   :urlToImage
                   "http://c1.nrostatic.com/sites/default/files/uploaded/trump1200.jpg",
                   :publishedAt "2018-01-24T09:00:00Z"})

(def art1 (assoc test-article :publishedAt "2018-01-24T09:00:00Z"))
(def art2 (assoc test-article :publishedAt "2018-01-24T09:30:00Z"))
(def art3 (assoc test-article :publishedAt "2018-01-24T10:00:00Z"))
(def art4 (assoc test-article :publishedAt "2018-01-24T10:30:00Z"))



(comment

  (get-articles "2018-01-24T08:30:00Z" "2018-01-24T11:50:00Z")
  (get-articles)

  (def counter (atom 0))
  (def sink (ms/stream))
  (ms/on-drained sink #(println "Drained..."))
  (ms/consume (partial process-number counter) sink)
  (future (streaming-daemon sink))
  (ms/close! sink)


  @(stream-numbers-to sink)

  @o

  (.close msg-sink)

  @(stream-numbers-to-failboat msg-sink)

  @(stream-numbers-to-failboat msg-sink)


  (.close msg-sink)

  (def wait



    )


  (def msg-sink (ms/stream))
  (def event-sink (ms/stream))
  (ms/consume #(println "Received message:" %) msg-sink)

  @(ms/put! event-sink :close)

  (future
    (md/loop [attempt 0]
      (let [{:keys [ok msg-source]} (stream-numbers-to event-sink msg-sink event-sink)]
        (if ok
          (do (println "Connection open. Waiting for events...")
              (case @(ms/take! event-sink)
                :drained (do (println "Drained... restarting.")
                             (md/recur 0))
                :close (do (println "Closing.")
                           (.close msg-source))))
          (println (str "Failed!")))))
    )



          (case status
          200
          420 (let [wait (math/expt 2 attempt)]
                (log/debug (str "Rate limited - sleeping for " wait " milliseconds."))
                (Thread/sleep wait)
                (md/recur (inc attempt)))
          (do (log/debug (str "Got a " status " - terminating."))))

  (ms/put! event-sink :close)

  (future-cancel t)


  (ms/drained? sink)
  (ms/closed? sink)

  (def stream (let [sink (ms/stream)]

                @(stream-numbers sink)))

  (ms/on-drained stream #(println "ON DRAINED: " ))

  (.close stream)



  (-> @(http/get "http://localhost:8001/streaming/numbers"
                 {:query-params {:count 3} :throw-exceptions false}))




  (def foo (->> @
            :body))

                                        ;=> (0 1 2 3 4 5 6 7 8 9)


  (def response @(http/get "http://localhost:8001/fake/tweets"
                           {:throw-exceptions false
                            :pool (http/connection-pool {:connection-options {:raw-stream? true}})}))

  response
  (bs/to-string @(ms/take! (:body response)))
  (mb/publish! (:fake-tweet-bus system) :all "woo!")

  stream

  (-> @(http/post "http://localhost:8001/fake/tweets"
                  {:headers {:content-type "application/json"}
                   :body (json/write-str {:username "mike"
                                          :text "you are the worst"})
                   :throw-exceptions false})
      (util/parse-json-body))

  (-> @(http/post "http://localhost:8001/fake/articles"
                  {:headers {:content-type "application/json"}
                   :body (json/write-str art4)})
      (util/parse-json-body))

  (-> @(http/get "http://localhost:8001/api/tweets"
                 {:socket-timeout 1000})
      (util/parse-json-body))

  (def conn @(http/websocket-client "ws://localhost:8001/streaming/tweets"))

  conn
  @(ms/take! conn)


  )
