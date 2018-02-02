(ns example.connection
  (:require [com.stuartsierra.component :as component]
            [manifold.stream :as ms]
            [taoensso.timbre :as log]))

(defprotocol ConnectionManager
  "Manages connections."
  (add! [this category conn] "Add a connection.")
  (close-all! [this] "Closes all connections."))

(defrecord AtomicConnectionManager [counter connections]
  ConnectionManager
  (add! [this category conn]
    (let [conn-id (swap! counter inc)]
      (swap! connections assoc conn-id {:id conn-id
                                        :category category
                                        :conn conn})
      conn-id))
  (close-all! [this]
    (let [all-conns (flatten (vals @connections))
          conn-count (count all-conns)]
      (when (pos? conn-count)
        (log/debug (str "Closing " conn-count " connections."))
        (doseq [entry all-conns]
          (ms/close! (:conn entry)))))))

(defn manager
  [config]
  (map->AtomicConnectionManager {:counter (atom 0)
                                 :connections (atom {})}))
