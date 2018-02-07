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

(def client (-> {:host (str "localhost:" "8001")
                 :content-type "application/json"}
                (client/client)))

(comment


  (client/articles client 2)
  (client/tweets client 1)


  )
