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

;; TODO: This might make 1 more call than necessary, depending on
;; how paging is implemented by the endpoint.
(defn lazy-pages
  ([get-page]
   (lazy-pages get-page 0 []))

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

(defn rand-str [len]
  (str/join (take len (repeatedly #(char (+ (rand 26) 65))))))
