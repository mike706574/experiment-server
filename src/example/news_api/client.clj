(ns example.news-api.client
  (:require [aleph.http :as http]
            [example.util :as util]
            [taoensso.timbre :as log]))

(defn request-articles
  [url api-key params]
  (-> @(http/get (str url "/everything") {:query-params params
                                          :headers {"Authorization" api-key}})
      (util/parse-json-body)))

(defn lazy-articles
  [url api-key params]
  (util/lazy-pages
   (fn [page-num]
     (let [params (assoc params :page page-num)
           response (request-articles url api-key params)
           page (-> response :body :articles)]
       (when-not (empty? page) page)))))

(defprotocol NewsApiClient
  (get-articles [this params]))

(defrecord BasicNewsApiClient [url api-key]
  NewsApiClient
  (get-articles [this params]
    (log/trace (str "Retrieving articles with params:" params))
    (lazy-articles url api-key params)))

(defn client [config]
  (let [{:keys [url api-key]} (:news-api-config config)]
    (map->BasicNewsApiClient {:url url :api-key api-key})))
