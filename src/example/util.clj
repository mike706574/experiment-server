(ns example.util
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn pretty
  [form]
  (with-out-str (clojure.pprint/pprint form)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn parse-json-body
  [response]
  (if-let [body (:body response)]
    (let [content-type (get-in response [:headers "content-type"])
          json? (and content-type (str/starts-with? content-type "application/json"))]
      (assoc response :body (if json?
                              (json/read (io/reader body) :key-fn keyword)
                              (slurp body))))
    response))

(defn lazy-pages
  ([get-page]
   (lazy-pages get-page 1 []))

  ([get-page num]
   (lazy-pages get-page num []))

  ([get-page num page]
   (lazy-seq
    (if (empty? page)
      (let [num (inc num)]
        (when-let [page (get-page num)]
          (cons (first page) (lazy-pages get-page num (rest page)))))
      (cons (first page) (lazy-pages get-page num (rest page)))))))

(defn flip [f] (fn [x y & args] (apply f y x args)))
