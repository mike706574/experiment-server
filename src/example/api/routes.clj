(ns example.api.routes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [compojure.api.sweet :as compojure]
            [compojure.response :refer [Renderable]]
            [example.news-api.repo :as news-api-repo]
            [example.twitter.repo :as twitter-repo]
            [manifold.stream :as ms]
            [ring.util.http-response :as response]
            [taoensso.timbre :as log]))

(extend-protocol Renderable
  manifold.deferred.IDeferred
  (render [d _] d))

(extend-protocol Renderable
  manifold.stream.SourceProxy
  (render [s _] s))


(defn routes [{:keys [news-api-repo tweet-repo] :as deps}]
  (compojure/context "/api" []
    (compojure/context "/tweets" []
      :tags ["tweets"]
      (compojure/resource
       {:get
        {:summary "retrieving tweets"
         :parameters {:query-params (s/keys :req-un [::page-number])}
         :responses {200 {:schema any?}}
         :handler (fn [{{:keys [page-number]} :query-params}]
                    (let [page-number (Integer/parseInt page-number)
                          tweets (twitter-repo/page tweet-repo page-number)]
                      (log/info (str "Returning tweets page " page-number "."))
                      (response/ok tweets)))}}))

    (compojure/context "/articles" []
      :tags ["articles"]
      (compojure/resource
       {:get
        {:summary "retrieving articles"
         :parameters {:query-params (s/keys :req-un [::page-number])}
         :responses {200 {:schema any?}}
         :handler (fn [{{:keys [page-number]} :query-params}]
                    (let [page-number (Integer/parseInt page-number)
                          tweets (news-api-repo/page news-api-repo page-number)]
                      (log/info (str "Returning articles page " page-number "."))
                      (response/ok tweets)))}}))))
