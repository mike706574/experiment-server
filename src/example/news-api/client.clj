(ns example.news-api.client
  (:require [aleph.http :as http]
            [example.util :as util]))

(defn get-request
  [url api-key params]
  (-> @(http/get url {:query-params params
                      :headers {"Authorization" api-key}})
      (util/parse-json-body)))

(defn everything
  [url api-key params]
  (get-request (str url "/everything") api-key params))

(defn lazy-everything
  [url api-key params]
  (util/lazy-pages
   (fn [page-num]
     (let [params (assoc params :page page-num)
           response (everything url api-key params)
           page (-> response :body :articles)]
       (when-not (empty? page) page)))))

(defprotocol NewsApiClient)
