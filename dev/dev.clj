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

(defn read-ig-systemconfig
  "read fern file, extract magic integrant/system symbol"
  ([]
   (read-ig-systemconfig (jio/resource "xiangqi-server.fern")))
  ([fname]
   (let [fern-env-thunk
         (fn []
             (binding [clojure.tools.reader/*data-readers*
                       (into @#'integrant.core/default-readers
                         clojure.tools.reader/default-data-readers)]
               (fe/load-environment fname)))

         config (f/evaluate (fern-env-thunk) 'integrant/system)]
     (integrant.repl/set-prep! #(ig/prep (f/evaluate (fern-env-thunk) 'integrant/system))))))

(defn presponse-for [& args]
  (apply io.pedestal.test/response-for
    (get-in system [:component/endpoint :io.pedestal.http/service-fn])
    args))

(read-ig-systemconfig)

(defn spy-to-atom
  [a v]
  (reset! a v)
  v)
