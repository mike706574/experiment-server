(ns example.fake.handler
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [compojure.core :as compojure :refer [GET POST]]
            [compojure.route :as route]
            [manifold.bus :as mb]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [manifold.time :as mt]
            [taoensso.timbre :as log]))

(defn periodically
  [stream period initial-delay f]
  (let [cancel (promise)]
    (deliver cancel
      (mt/every period initial-delay
        (fn []
          (try
            (let [d (if (ms/closed? stream)
                      (md/success-deferred false)
                      (ms/put! stream (f)))]
              (if (realized? d)
                (when-not @d
                  (@cancel)
                  (ms/close! stream))
                (do
                  (@cancel)
                  (md/chain' d
                    (fn [x]
                      (if-not x
                        (ms/close! stream)
                        (periodically stream period (- period (rem (System/currentTimeMillis) period)) f)))))))
            (catch Throwable e
              (@cancel)
              (ms/close! stream)
              (log/error e "error in 'periodically' callback"))))))))

(def news-api-date-format (time-format/formatter "yyyy-MM-dd"))
(def news-api-time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(defn parse-time [s]
  (try
    (time-format/parse news-api-date-format s)
    (catch IllegalArgumentException ex
      (time-format/parse news-api-time-format s))))

(defn get-articles [fake-articles {params :params :as req}]
  (log/trace (str "Retrieving articles based on the following criteria: " params))
  (let [{:strs [from to page]} params
        page (if page (Integer/parseInt page) 1)
        pred (fn [article]
               (let [from (some-> from parse-time)
                     to (some-> to parse-time)
                     published (parse-time (:publishedAt article))
                     after-from (if from
                                  (time/after? published from)
                                  true)
                     before-to (if to
                                 (time/before? published to)
                                 true)]
                 (and after-from before-to)))
        all-articles @fake-articles
        articles (->> all-articles
                  (filter pred)
                  (drop (* (dec page) 20))
                  (take 20))]
    (log/trace (str "Returning " (count articles) " of " (count all-articles) " articles."))
    {:status 200
     :headers {"content-type" "application/json"}
     :body (json/write-str {:status "ok"
                            :totalResults (count articles)
                            :articles articles})}))

(defn raw-fake-tweet [{:keys [text username]}]
  (-> {:text text
       :user {:screen_name username}}
      (assoc :id (rand-int 1000000))
      (json/write-str)
      (str "\r\n")))

(defn send-fake-tweet [fake-tweet-bus tweet]
  (mb/publish! fake-tweet-bus :all (raw-fake-tweet tweet)))

(s/def :twitter/text string?)
(s/def :twitter/username string?)
(s/def :twitter/tweet (s/keys :req-un [:twitter/text
                                       :twitter/username]))

(defn handler
  [{:keys [fake-tweet-bus]}]
  (let [fake-tweets (atom [])
        fake-articles (atom [])
        get-articles (partial get-articles fake-articles)]
    (compojure/routes

     (POST "/fake/tweets/streaming" req
          (let [stream (ms/stream 1)]
            (periodically stream 9000 9000 (constantly "\r\n"))
            (ms/connect (mb/subscribe fake-tweet-bus :all) stream)
            {:status 200
             :headers {"content-type" "text/plain"}
             :body stream}))

     (POST "/fake/tweets" {fake-tweet :body}
           (if-let [error (s/explain-data :twitter/tweet fake-tweet)]
             {:status 400
              :headers {"content-type" "application/json"}
              :body (json/write-str error)}
             (do
               (log/debug (str "Sending fake tweet:" fake-tweet))
               (send-fake-tweet fake-tweet-bus fake-tweet)
               (swap! fake-tweets conj fake-tweet)
               {:status 201})))

     (GET "/fake/tweets" req
          {:status 200
           :headers {"content-type" "application/json"}
           :body (json/write-str {:tweets @fake-tweets})})

     (GET "/fake/articles" [] get-articles)

     (GET "/fake/articles/everything" [] get-articles)

     (POST "/fake/articles" {:keys [body]}
           (let [article (if (nil? (:publishedAt body))
                           (assoc body :publishedAt (time-format/unparse news-api-time-format (time/now)))
                           body)]
             (swap! fake-articles conj article)
             {:status 201}))
     (route/not-found "No such page."))))
