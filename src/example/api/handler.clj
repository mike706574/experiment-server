(ns example.api.handler
  (:require [compojure.api.sweet :as compojure]
            [example.api.routes :as routes]))

(defn handler [deps]
  (compojure/api
   {:coercion :spec
    :exceptions {:handlers {:compojure.api.exception/default (fn [x]
                                                               (println x))}}
    :swagger {:ui "/api/docs"
              :spec "/api/swagger.json"
              :data {:info {:title "greeting"
                            :description "an api"}
                     :tags [{:name "hello", :description "Hello"}]}}}
   (routes/routes deps)))
