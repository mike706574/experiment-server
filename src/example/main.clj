(ns example.main
  (:require [example.system :as system]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:gen-class :main true))

(def config {:service/id "example-server"
             :service/port nil
             :service/log-path "/tmp"
             :service/secret-key "secret"
             :service/user-manager-type :atomic
             :service/users {"mike" "rocket"}})

(defn -main
  [& [port]]
  (log/set-level! :debug)
  (let [port (Integer. (or port (env :port) 5000))]
    (log/info (str "Using port " port "."))
    (let [system (system/system (merge config {:service/port port}))]
      (log/info "Starting system.")
      (component/start-system system)
      (log/info "Waiting forever.")
      @(promise))))
