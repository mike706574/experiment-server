(ns news
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

   [clj-time.core :as time]
   [clj-time.format :as time-format]

   [byte-streams :as bs]

   [manifold.stream :as ms]
   [manifold.deferred :as md]

   [aleph.http :as http]
   [taoensso.timbre :as log]

   [clojure.data.json :as json]

   [example.users :as users]
   [example.system :as system]
   [example.util :as util]))

(log/set-level! :debug)

(def api-key "58ef69549bf64f55a5dbdebefcdcf47f")
(def url "https://newsapi.org/v2/")
(def props {:url url :api-key api-key})

(defn get-request
  [url api-key params]
  (-> @(http/get url {:query-params params
                      :headers {"Authorization" api-key}
                      :throw-exceptions false})
      (util/parse-json-body)))

(defn top-headlines
  [{:keys [url api-key]} params]
  (get-request (str url "top-headlines") api-key params))

(defn everything
  [{:keys [url api-key]} params]
  (get-request (str url "everything") api-key params))

(defn lazy-top-headlines
  [props params]
  (util/lazy-pages
   (fn [page-num]
     (let [params (assoc params :page page-num)
           response (top-headlines props params)
           page (-> response :body :articles)]
       (when-not (empty? page) page)))))

(defn lazy-top-headlines
  [props params]
  (util/lazy-pages
   (fn [page-num]
     (let [params (assoc params :page page-num)
           response (top-headlines props params)
           page (-> response :body :articles)]
       (when-not (empty? page) page)))))

(defn lazy-everything
  [props params]
  (util/lazy-pages
   (fn [page-num]
     (let [params (assoc params :page page-num)
           response (everything props params)
           page (-> response :body :articles)]
       (when-not (empty? page) page)))))

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss"))
(def parse (partial time-format/parse time-format))
(def unparse (partial time-format/unparse time-format))

(comment


(def x (time/now))
(time/minus x five-minutes)

(time/ago (time/minutes 5))

(time/minus (time/now) (time/minutes 10))

(org.joda.time.Period. 342)


  (def fifteen (take 15 (lazy-top-headlines props {:country "us"})))

  (top-headlines props {:country "us"})

  fifteen
  (everything props {:language "en" :q "trump"})

  (def tf (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss"))

  (time-format/unparse tf (time/now))

  (count (take 75 (lazy-everything props {:language "en" :q "foo" :from "2018-01-29T03:15:00"})))

  (nth (take 25 (lazy-everything props {:language "en" :q "trump"})) 20)
  )
