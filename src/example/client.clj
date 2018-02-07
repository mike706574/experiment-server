(ns example.client
  (:require [aleph.http :as http]
            [example.users :as users]
            [example.util :as util]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn add-user!
  [system username password]
  (users/add! (:user-manager system) {:service/username username
                                      :service/password password}))

(defn http-url [host] (str "http://" host))

(defn get-token
  [host credentials]
  (let [response (util/parse-json-body
                  @(http/post (str (http-url host) "/api/tokens")
                              {:headers {"Content-Type" "application/json"
                                         "Accept" "text/plain"}
                               :body (json/write-str credentials)
                               :throw-exceptions false}))]
    (case (:status response)
      201 (-> response :body :token)
      401 nil
      (throw (ex-info "Failed to fetch token." {:username (:username credentials)
                                                :response response})))))

(defprotocol Client
  (authenticate [this credentials])
  (articles [this page-number])
  (tweets [this page-number]))

(defrecord ServiceClient [host token]
  Client
  (authenticate [this credentials]
    (when-let [token (get-token host credentials)]
      (assoc this :token token)))

  (articles [this page-number]
    (util/parse-json-body @(http/get (str (http-url host) (str "/api/articles"))
                                     {:headers {"Content-Type" "application/json"
                                                "Accept" "application/json"}
                                      :query-params {"page-number" page-number}
                                      :throw-exceptions false})))

  (tweets [this page-number]
    (util/parse-json-body @(http/get (str (http-url host) (str "/api/tweets"))
                                     {:headers {"Content-Type" "application/json"
                                                "Accept" "application/json"}
                                      :query-params {"page-number" page-number}
                                      :throw-exceptions false}))))

(defn client
  [{:keys [host content-type]}]
  (map->ServiceClient {:host host
                       :content-type content-type}))

(comment
  (-> @(http/get "http://localhost:8001/api/greetings" {:throw-exceptions false})
      (:body)
      (io/reader)
      (json/read :key-fn keyword))

  (-> @(http/get "http://localhost:8001/api/greetings?name=mike" {:throw-exceptions false})
      (:body)
      (io/reader)
      (json/read :key-fn keyword)))
