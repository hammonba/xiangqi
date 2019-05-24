(ns xiangqi.board-service
  (:require [datomic.client.api :as dca]
            [xiangqi.datomic.peer :as dp]
            [xiangqi.board-ident :as board-ident]
            [xiangqi.datomic.tx-moves :as tx-moves]
            [xiangqi.board-utils :as board-utils]
            [xiangqi.bit-utils :as bit-utils]
            [medley.core :as medley]
            [xiangqi.board-ident :as bi]
            [xiangqi.pedestal.websockets]))

(def board-msg
  (pr-str {:msg-type :board
           :disposition [{:x "0", :y "6", :piecename "red-soldier" :moves [{:x "0" :y "6"}]}
                         {:x "2", :y "6", :piecename "red-soldier"}
                         {:x "4", :y "6", :piecename "red-soldier"}
                         {:x "6", :y "6", :piecename "red-soldier"}
                         {:x "8", :y "6", :piecename "red-soldier"}
                         {:x 1, :y 7, :piecename "red-cannon" :moves [{:x 1 :y 8} {:x 1 :y 6} {:x 1 :y 5} {:x 1 :y 4} {:x 1 :y 3} {:x 1 :y 0}]}
                         {:x "7", :y "7", :piecename "red-cannon"}
                         {:x "0", :y "9", :piecename "red-chariot"}
                         {:x "8", :y "9", :piecename "red-chariot"}
                         {:x "1", :y "9", :piecename "red-horse"}
                         {:x "7", :y "9", :piecename "red-horse"}

                         {:x "2", :y "9", :piecename "red-minister"}
                         {:x "6", :y "9", :piecename "red-minister"}
                         {:x "3", :y "9", :piecename "red-guard"}
                         {:x "5", :y "9", :piecename "red-guard"}
                         {:x "4", :y "9", :piecename "red-marshal"}

                         {:x "0", :y "3", :piecename "black-private"}
                         {:x "2", :y "3", :piecename "black-private"}
                         {:x "4", :y "3", :piecename "black-private"}
                         {:x "6", :y "3", :piecename "black-private"}
                         {:x "8", :y "3", :piecename "black-private"}

                         {:x "1", :y "2", :piecename "black-catapult"}
                         {:x "7", :y "2", :piecename "black-catapult"}
                         {:x "0", :y "0", :piecename "black-chariot"}
                         {:x "8", :y "0", :piecename "black-chariot"}

                         {:x "1", :y "0", :piecename "black-horse"}
                         {:x "7", :y "0", :piecename "black-horse"}

                         {:x "2", :y "0", :piecename "black-elephant"}
                         {:x "6", :y "0", :piecename "black-elephant"}

                         {:x "3", :y "0", :piecename "black-guard"}
                         {:x "5", :y "0", :piecename "black-guard"}
                         {:x "4", :y "0", :piecename "black-general"}]}))

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
   :board-ident ident
   :board/player player})

(defmethod xiangqi.pedestal.websockets/websocket-ontext :get-board
  [sendch {:keys [board-ident]}]
  (->
    (or (bit-utils/decodestring-into-bigint board-ident)
      bi/initial-board-218)
    board-by-ident
    transform-for-websocket
    create-websocket-message
    (pr-str)))
