(ns xiangqi.board-service
  "hook into websocket messaging to deliver board data"
  (:require [datomic.client.api :as dca]
            [xiangqi.datomic.peer :as dp]
            [xiangqi.board.board-ident :as board-ident
             :only [decord-board-ident initial-board-218]]
            [xiangqi.datomic.tx-moves :as tx-moves]
            [xiangqi.board.bit-utils :as bit-utils]
            [medley.core :as medley]
            [xiangqi.pedestal.websockets]))

(defn datomic-conn [] @dp/conn)

(defn datomic-db
  []
  (dca/db (datomic-conn)))

(defn board-by-ident
  [board-ident]
  (-> board-ident
    board-ident/decode-boardident-218
    (assoc :conn (datomic-conn))
    tx-moves/addmoves-to-board))

(defn transform-endloc
  [{:location/keys [x y] :keys [next-board]}]
  {:move/x x :move/y y :move/next-board (bit-utils/encodebigint-as-string next-board)})

(defn transform-agg-endloc
  [agg]
  (map transform-endloc (:end-locations agg)))

(defn transform-piece
  [piece]
  {:piece/piecename (name (:disposition/piece piece))
   :location/x (:location/x piece)
   :location/y (:location/y piece)
   :piece/location (:disposition/location piece)
   :piece/moves (mapcat transform-agg-endloc (:end-locations piece))})

(defn transform-for-websocket
  [{:board/keys [disp-vec] :as board}]
  (assoc board :board/disposition
               (into [] (comp
                          (filter :disposition/piece )
                          (map transform-piece))
                 disp-vec)))

(defn create-websocket-message
  [{:board/keys [ident player disposition]}]
  {:msg-type :board
   :disposition disposition
   :board-ident (bit-utils/encodebigint-as-string ident)
   :board/player player})

(defmethod xiangqi.pedestal.websockets/websocket-ontext :get-board
  [sendch {:keys [board-ident]}]
  (->
    (or (bit-utils/decodestring-into-bigint board-ident)
      board-ident/initial-board-218)
    board-by-ident
    transform-for-websocket
    create-websocket-message
    #_(pr-str)))
