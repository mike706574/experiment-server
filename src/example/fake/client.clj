(ns example.fake.client
  (:require [aleph.http :as http]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [example.util :as util]))

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(def unparse (partial time-format/unparse time-format))

(defn add-article [url article]
  (let [timestamped-article (assoc article :publishedAt (unparse (time/now)))]
    (util/parse-json-body @(http/post (str url "/articles") {:headers {:content-type "application/json"}
                                           :body (json/write-str article)})
        )))

(defn get-articles [url article]
  (-> @(http/get (str url "/articles") {:headers {:content-type "application/json"}})
      (util/parse-json-body)))

(defn add-tweet [url tweet]
  (-> @(http/post (str url "/tweets")
                  {:headers {:content-type "application/json"}
                   :body (json/write-str tweet)})
      (util/parse-json-body)))

(defn get-tweets [url]
  (-> @(http/get (str url "/tweets"))
      (util/parse-json-body)))

(def apple-articles
  (->> (json/read-str (slurp "dev-resources/apple-articles-1.json") :key-fn keyword)
       (map #(dissoc % :publishedAt))))

(defn add-rand-apple-article [url]
  (add-article url (rand-nth apple-articles)))

(defn add-rand-tweet [url]
  (add-tweet url {:username (util/rand-str (rand-nth (range 5 15)))
                  :text (util/rand-str (rand-nth (range 10 142)))}))

(def apple-tweets
  (->> "dev-resources/apple-tweets-1.json"
       (io/reader)
       (line-seq)
       (map #(json/read-str % :key-fn keyword))))

(defn add-rand-apple-tweet [url]
  (add-tweet url (rand-nth apple-tweets)))


  (->> "dev-resources/apple-tweets-1.json"
       (io/reader)
       (line-seq)
       (rand-nth)
       )
