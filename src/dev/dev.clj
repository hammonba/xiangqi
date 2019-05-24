(ns dev
  (:require [io.pedestal.http]
            [xiangqi.datomic.client :as dc]
            [xiangqi.datomic.tx-moves :as xtx]
            [xiangqi.board-ident :as bi]
            [xiangqi.board-utils :as bu]
            [xiangqi.movecalc :as mc]
            [xiangqi.pedestal.server :as xiangqi-server]
            [xiangqi.board-service]))

(def server (atom nil))

(defn run-dev
  []
  (reset! server (xiangqi-server/run-dev)))

(defn start
  []
  (reset! server (io.pedestal.http/start xiangqi-server/runnable-service)))

(defn stop
  []
  (reset! server (io.pedestal.http/stop @server)))
