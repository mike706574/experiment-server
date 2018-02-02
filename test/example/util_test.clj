(ns example.util-test
  (:require [clojure.test :refer [deftest is]]
            [example.util :as util]))

(deftest pretty
  (is (= "{:foo \"bar\", :baz \"boo\"}\n"
         (util/pretty {:foo "bar"
                       :baz "boo"}))))

(deftest uuid
  (is (string? (util/uuid))))
