(ns example.news-api.repo)

(defn same-article? [a b]
  (and (= (:title a) (:title b))
       (= (:publishedAt a) (:publishedAt b))
       (= (:source a) (:source b))))

(defprotocol NewsApiRepo
  (store! [this article])
  (has? [this article])
  (all [this]))

(defrecord AtomicNewsApiRepo [articles counter]
  NewsApiRepo
  (store! [this article]
    (let [id (str (swap! counter inc))
          article (assoc article :id id)]
      (swap! articles conj article)
      article))
  (has? [this article]
    (not (nil? (first (filter #(same-article? %1 article) @articles)))))
  (all [this]
    @articles))

(defn atomic [config]
  (map->AtomicNewsApiRepo {:counter (atom 0)
                           :articles (atom [])}))

(swap! (atom 1) inc)

(same-article?
 {:source {:id "reuters", :name "Reuters"}, :author "Reuters Editorial", :title "FDA's tobacco stance faces test with Philip Morris iQOS device", :description "WASHINGTON (Reuters) - In a decision expected to test the Trump administration's approach to tobacco regulation, U.S. health advisers will vote this week on whether to allow Philip Morris International Inc to sell its novel iQOS tobacco device and claim it is…", :url "https://www.reuters.com/article/us-health-tobacco-pmi/fdas-tobacco-stance-faces-test-with-philip-morris-iqos-device-idUSKBN1FB0J2", :urlToImage "https://s3.reutersmedia.net/resources/r/?m=02&d=20180122&t=2&i=1224061779&w=1200&r=LYNXMPEE0L0DL", :publishedAt "2018-02-03T22:50:28Z", :id "9"}
  {:source {:id "reuters", :name "Reuters"}, :author "Reuters Editorial", :title "FDA's tobacco stance faces test with Philip Morris iQOS device", :description "WASHINGTON (Reuters) - In a decision expected to test the Trump administration's approach to tobacco regulation, U.S. health advisers will vote this week on whether to allow Philip Morris International Inc to sell its novel iQOS tobacco device and claim it is…", :url "https://www.reuters.com/article/us-health-tobacco-pmi/fdas-tobacco-stance-faces-test-with-philip-morris-iqos-device-idUSKBN1FB0J2", :urlToImage "https://s3.reutersmedia.net/resources/r/?m=02&d=20180122&t=2&i=1224061779&w=1200&r=LYNXMPEE0L0DL", :publishedAt "2018-02-03T22:50:28Z", :id "9"})
