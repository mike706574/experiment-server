(ns example.news-api.daemon
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [aleph.http :as http]
            [byte-streams :as bs]
            [example.util :as util]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [manifold.time :as mt]

            [taoensso.timbre :as log]
            [example.twitter.auth :as auth]))

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

(def time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))
(def parse (partial time-format/parse time-format))
(def unparse (partial time-format/unparse time-format))

(defn every [url api-key params interval]
  (let []
    (future
      (loop [from (time/ago (time/seconds (/ interval 1000)))]
        (let [now (time/now)
              params-with-range (assoc params :from (unparse last) :to (unparse now))
              articles (lazy-everything url api-key params-with-range)]
          (if (empty? articles)
            (println "No articles.")
            (for [article articles]
              (println "New article:" (:title article))))
          (Thread/sleep interval)
          (recur now))))))

(def api-key "58ef69549bf64f55a5dbdebefcdcf47f")
(def url "https://newsapi.org/v2")

(def fake-url "http://localhost:8001/fake/articles")

(defn add-article [url article]
  (-> @(http/post url {:headers {:content-type "application/json"}
                       :body (json/write-str article)})
      (util/parse-json-body)))

(defn get-articles
  ([url]
   (get-articles url {}))
  ([url params]
   (-> @(http/get url {:query-params params})
       (util/parse-json-body)))
  ([url from to]
   (get-articles url {"from" from "to" to})))

(defn test-article []
  {:source {:id nil, :name "Realclearpolitics.com"},
   :author "Joel Kotkin, City Journal",
   :title "Can the Trump Economy Trump Trump?",
   :description
   "Joel Kotkin, City Journal The president's economic policies are showing marked success, but he may not benefit politically unless he learns how to get out of his own way.",
   :url
   "https://www.realclearpolitics.com/2018/01/18/can_the_trump_economy_trump_trump_431798.html",
   :urlToImage nil,
   :publishedAt (unparse (time/now))})


(comment
  (add-article fake-url (test-article))
  (get-articles fake-url)
  (def fut (every fake-url api-key {} 5000))
  (future-cancel fut)
  fut
  (test-article)

  fut
  (def x (everything url api-key {:language "en"
                                  :q "apple"
                                  :sources "the-wall-street-journal,reuters,financial-times,the-economist"
                                  :from "2018-01-29"}))

  (take 10 x)


  (

 {:source {:id nil, :name "Freerepublic.com"},
  :author "Rush Limbaugh.com",
  :title "One Year Later: A Look Back at the Election",
  :description
  "RUSH: Twelve Months Later, Trump Would Probably Still Win the 2016 Election. Thats the headline of a piece in the Washington Post today. This is a bombshell story, when you measure this against nearly universal coverage from the Drive-By Media for the past…",
  :url "https://www.freerepublic.com/focus/f-news/3602619/posts",
  :urlToImage nil,
  :publishedAt "2017-11-07T22:34:46Z"}
 {:source {:id "cnn", :name "CNN"},
  :author nil,
  :title "Tapper: President Trump contradicts candidate Trump",
  :description
  "President Donald Trump's personal attorney, John Dowd, said that the President cannot be guilty of obstructing justice, according to an interview with Axios. CNN's Jake Tapper looks at the latest developments in this story.",
  :url
  "http://www.cnn.com/videos/politics/2017/12/04/trump-flynn-obstruction-of-justice-questions-tapper-monologue-lead.cnn",
  :urlToImage
  "http://cdn.cnn.com/cnnnext/dam/assets/171204114521-02-donald-trump-12-04-2017-super-tease.jpg",
  :publishedAt "2017-12-04T22:04:09Z"}
 {:source {:id nil, :name "Eschatonblog.com"},
  :author "noreply@blogger.com (Atrios)",
  :title "Trump Voters Who Still Like Trump",
  :description
  "Have we had one of those since yesterday's election? I would like every editor who spent a year assigning those to answer the simple question: why? We never had those pieces in the Obama years.",
  :url
  "http://www.eschatonblog.com/2017/12/trump-voters-who-still-like-trump.html",
  :urlToImage nil,
  :publishedAt "2017-12-13T21:17:00Z"}
 {:source {:id "fox-news", :name "Fox News"},
  :author nil,
  :title "Bossie, Lewandowski talk 'Let Trump Be Trump'",
  :description "Authors discuss their new book on 'Hannity.'",
  :url "http://video.foxnews.com/v/5667876136001/",
  :urlToImage
  "http://a57.foxnews.com/media2.foxnews.com/BrightCove/694940094001/2017/12/05/640/360/694940094001_5667918123001_5667876136001-vs.jpg",
  :publishedAt "2017-12-05T03:02:59Z"}
 {:source {:id "daily-mail", :name "Daily Mail"},
  :author
  "http://www.dailymail.co.uk/home/search.html?s=&authornamef=Carly+Stern+For+Dailymail.com, By Carly Stern For Dailymail.com",
  :title "Ivanka Trump store opens at Trump Tower",
  :description
  "The small store opened at the Fifth Avenue building late Thursday afternoon. It features pink walls with just five shelves, all stocked with bags.",
  :url
  "http://www.dailymail.co.uk/femail/article-5180835/Ivanka-Trump-store-opens-Trump-Tower.html",
  :urlToImage
  "http://i.dailymail.co.uk/i/pix/2017/12/14/21/4756FBB600000578-0-image-a-42_1513287647540.jpg",
  :publishedAt "2017-12-14T21:49:16Z"}
 {:source {:id nil, :name "Latimes.com"},
  :author "Los Angeles Times",
  :title "Today: Trump Meets ‘the Trump of Asia’",
  :description
  "President Trump is on the final leg of his five-nation tour of Asia, one that’s included many red carpets, “two or three very short conversations” with Vladimir Putin and some Twitter outbursts. TOP STORIES Trump Meets ‘the Trump of Asia’ President Trump is i…",
  :url
  "http://www.latimes.com/newsletters/la-me-todays-headlines-20171113-story.html",
  :urlToImage
  "http://www.trbimg.com/img-5a0987a2/turbine/la-me-todays-headlines-20171113",
  :publishedAt "2017-11-13T13:00:00Z"}
 {:source {:id nil, :name "Themillions.com"},
  :author "Adam Boretz",
  :title "Surviving Trump: The KKK and Donald Trump",
  :description
  "Klan feminism does illustrate something very important about the support many women gave to Trump, and that is that we cannot assume that women’s concerns with gender issues are always their most prominent concerns. The post Surviving Trump: The KKK and Donal…",
  :url
  "http://themillions.com/2017/11/surviving-trump-the-kkk-and-donald-trump.html",
  :urlToImage
  "http://images.amazon.com/images/P/1631493698.01.LZZZZZZZ.jpg",
  :publishedAt "2017-11-02T16:00:50Z"}
 {:source {:id "cnn", :name "CNN"},
  :author "Jackie Wattles",
  :title "Trump Organization and Trump SoHo cut ties",
  :description
  "The owner of the struggling Trump SoHo building in New York City reached a deal to end its contract with the Trump Organization early.",
  :url
  "http://money.cnn.com/2017/11/22/news/trump-soho-organization-contract-buyout/index.html",
  :urlToImage
  "http://i2.cdn.turner.com/money/dam/assets/170427134536-trump-soho-hotel-780x439.jpg",
  :publishedAt "2017-11-23T01:15:46Z"})

  )
