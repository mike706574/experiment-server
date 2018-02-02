(defproject fun.mike/example-server "0.0.1-SNAPSHOT"
  :description "A project."
  :url "https://github.com/mike706574/example-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.stuartsierra/component "0.3.2"]

                 ;; Utility
                 [environ "1.1.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [metosin/spec-tools "0.5.1"]
                 [clj-time "0.14.2"]

                 [manifold "0.1.6"]
                 [byte-streams "0.2.3"]
                 [twttr "2.0.0"]
                 [clj-oauth "1.5.5"]

                 ;; Web
                 [aleph "0.4.4"]
                 [ring/ring-anti-forgery "1.1.0"]
                 [ring-cors "0.1.11"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [metosin/compojure-api "2.0.0-alpha18"]
                 [compojure "1.6.0"]
                 [metosin/ring-http-response "0.9.0"]

                 ;; Security
                 [buddy/buddy-hashers "1.3.0"]
                 [buddy/buddy-sign "2.2.0"]
                 [fun.mike/azure-auth-alpha "0.0.1-SNAPSHOT"]
                 [com.nimbusds/oauth2-oidc-sdk "5.41.1"]]
  :plugins [[org.clojure/tools.nrepl "0.2.12"]
            [lein-cloverage "1.0.10"]]
  :profiles {:dev {:source-paths ["dev"]
                   :target-path "target/dev"
                   :dependencies [[org.clojure/test.check "0.10.0-alpha2"]
                                  [org.clojure/tools.namespace "0.2.11"]]}
             :production {:aot :all
                          :main example.main
                          :uberjar-name "example.jar"}})
