(ns example.azure-auth
  (:import [fun.mike.azure.auth Authenticator]))

(def authenticator (Authenticator. "6eddd185-7ad7-4ee3-9481-04644b9f43a2"
                                   "f9e527eb-55e3-4f45-b549-f6379f328245"))

(.getMessage (.authenticate authenticator "Bearer 12345"))
