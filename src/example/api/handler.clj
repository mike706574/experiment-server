(ns example.api.handler
  (:require [compojure.api.sweet :as compojure]
            [example.api.routes :as routes]
            [ring.middleware.cors :refer [wrap-cors]]))

(defn handler [deps]
  (-> {:coercion :spec
       :exceptions {:handlers {:compojure.api.exception/default (fn [x]
                                                                  (println x))}}
       :swagger {:ui "/api/docs"
                 :spec "/api/swagger.json"
                 :data {:info {:title "greeting"
                               :description "an api"}
                        :tags [{:name "hello", :description "Hello"}]}}}
      (compojure/api (routes/routes deps))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete])))
