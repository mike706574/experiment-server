(ns example.api.routes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [compojure.api.sweet :as compojure]
            [compojure.response :refer [Renderable]]
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

(defn routes [{:keys [tweet-repo]}]
  (compojure/context "/api" []
    (compojure/context "/tweets" []
      :tags ["tweets"]
      (compojure/resource
       {:get
        {:summary "retrieving tweets"
         :responses {200 {:schema any?}}
         :handler (fn [{{:keys [name]} :query-params}]
                    (let [tweets (twitter-repo/all tweet-repo)]
                      (log/info (str "Returning " (count tweets) " tweets."))
                      (response/ok tweets)))}}))))
