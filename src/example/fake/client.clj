(ns example.fake.client
  (:require [aleph.http :as http]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.data.json :as json]
            [example.util :as util]))

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(def unparse (partial time-format/unparse time-format))

(defn add-article [url article]
  (let [timestamped-article (assoc article :publishedAt (unparse (time/now)))]
    (-> @(http/post (str url "/articles") {:headers {:content-type "application/json"}
                                           :body (json/write-str article)})
        (util/parse-json-body))))

(defn get-articles [url article]
  (let [timestamped-article (assoc article)]
    (-> @(http/get (str url "/articles") {:headers {:content-type "application/json"}})
        (util/parse-json-body))))

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
