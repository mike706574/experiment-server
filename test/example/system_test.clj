(ns example.system-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [example.system :as system]
            [example.client :as client]
            [example.macros :refer [with-system unpack-response]]
            [example.util :as util]
            [taoensso.timbre :as log]))

(log/set-level! :trace)

(def port 9001)

(def config {:service/id "example-server"
             :service/port port
             :service/log-path "/tmp"
             :service/user-manager-type :atomic
             :service/users {"mike" "rocket"}})

(deftest greeting
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type "application/json"}
                     (client/client))]
      (unpack-response (client/greeting client "mike")
        (is (= 200 status))
        (is (= {:greeting "Hello, mike!"} body))))))

(deftest authenticates-successfully
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type "application/json"}
                     (client/client)
                     (client/authenticate {:username "mike"
                                           :password "rocket"}))
          token (:token client)]
      (is (not (str/blank? token))))))

(deftest fails-to-authenticate
  (with-system (system/system config)
    (let [client (-> {:host (str "localhost:" port)
                      :content-type "application/json"}
                     (client/client)
                     (client/authenticate {:username "mike"
                                           :password "kablam"}))
          token (:token client)]
      (is (nil? token)))))
