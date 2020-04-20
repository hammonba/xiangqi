(ns dev
  (:require
   [clojure.java.io :as jio]
   [clojure.tools.namespace.repl :as tnr]
   [clojure.repl :refer :all]
   [datomic.api :as d]
   [integrant.core :as ig]
   [integrant.repl :refer [init go halt clear reset reset-all]]
   [integrant.repl.state :as ig.state :refer [system config]]
   [io.pedestal.test :refer :all]
   [fern :as f]
   [fern.easy :as fe]
   [fipp.edn :refer [pprint]]
   [xiangqi.http-server]
   [xiangqi.vase-api]
   [xiangqi.websocket-api]
   [integrant.repl.state :as state]
   [com.cognitect.vase.try :as try :refer [try->]]
   [com.cognitect.vase.fern :as fern]
   [com.cognitect.vase.api :as a]))



(defn delete-inmem-datomic
  []
  (run!
    d/delete-database
    (d/get-database-names "datomic:mem://*")))

(defn integrant-fern-env
  "load fern environment with integrant data-readers enabled"
  ([path]
   (integrant-fern-env path nil))
  ([path plugin-symbol]
   (binding [clojure.tools.reader/*data-readers*
             (into @#'integrant.core/default-readers
               clojure.tools.reader/default-data-readers)]
     (fe/load-environment path plugin-symbol))))

(defn read-ig-systemconfig
  "read fern file, extract magic integrant/system symbol"
  ([]
   (read-ig-systemconfig (jio/resource "xiangqi-server.fern")))
  ([fname]
   (integrant.repl/set-prep!
     #(ig/prep (f/evaluate (integrant-fern-env fname)
                 'integrant/system)))))

(defn presponse-for [& args]
  (apply io.pedestal.test/response-for
    (get-in system [:component/endpoint :io.pedestal.http/service-fn])
    args))

(read-ig-systemconfig)

(defn spy-to-atom
  [a v]
  (reset! a v)
  v)

(def datomic-client
  (datomic.client.api/client
    {:server-type :peer-server
     :access-key "myaccesskey"
     :secret "mysecret"
     :endpoint "localhost:8998"
     :validate-hostnames false}))
(def datomic-game-conn
  (datomic.client.api/connect datomic-client {:db-name "game"}))
(def datomic-user-conn
  (datomic.client.api/connect datomic-client {:db-name "user"}))

(defn reset-database
  [dbname]
  (let [uri (format "datomic:free://localhost:4334/%s" dbname)]
    (datomic.api/delete-database uri)
    (datomic.api/create-database uri)))

(defn reset-databases
  []
  (run! reset-database ["game" "board" "user"])
  (println "now DONT FORGET to restart the Peer Server!"))
