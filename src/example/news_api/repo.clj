(ns example.news-api.repo
  (:require [clojure.spec.alpha :as s]))

(defn same-article? [a b]
  (and (= (:title a) (:title b))
       (= (:published-at a) (:published-at b))
       (= (:source a) (:source b))))

(defprotocol NewsApiRepo
  (-store! [this article])
  (-has? [this article])
  (-page [this number])
  (-all [this]))

(defrecord AtomicNewsApiRepo [articles counter]
  NewsApiRepo
  (-store! [this article]
    (let [id (str (swap! counter inc))
          article (assoc article :id id)]
      (swap! articles conj article)
      article))
  (-has? [this article]
    (not (nil? (first (filter #(same-article? %1 article) @articles)))))
  (-page [this number]
    (take 10 (drop (* 10 (dec number)) @articles)))
  (-all [this]
    @articles))

(defn atomic [config]
  (map->AtomicNewsApiRepo {:counter (atom 0)
                           :articles (atom [])}))

(defn store! [repo article] (-store! repo article))
(defn has? [repo article] (-has? repo article))
(defn page [repo number] (-page repo number))
(defn all [repo] (-all repo))

(s/fdef page
  :args (s/cat :repo any? :number integer?))
