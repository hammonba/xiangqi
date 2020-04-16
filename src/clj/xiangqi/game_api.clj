(ns xiangqi.game-api
  (:require [io.pedestal.log :as log]
            [datomic.client.api :as client]
            [medley.core :as medley]
            [xiangqi.board-api :as board-api]
            [xiangqi.board.board-ident :as board-ident]
            [clojure.tools.logging :as clog])
  (:import [java.util UUID]))

#_(def da (atom nil))

(def board-tempid "boardentity-tempid")

(defn board-entities
  "ensure that entity exists for board by transacting it; return its entity id"
  [conn boards & other-datoms]
  (let [board-datoms
        (map (fn [[n b]]
                 (->
                   (select-keys b [:board/ident-string :board/ident-218])
                   (assoc :db/id n)))
          boards)]
    (-> conn
      (client/transact {:tx-data (concat board-datoms other-datoms)})
      :tempids
      (select-keys (keys boards)))))

#_(defn board-entity
  "ensure that entity exists for board by transacting it; return its entity id"
  [conn board & other-datoms]
  (let [board-datoms (-> board
                       (select-keys [:board/ident-string :board/ident-218])
                       (assoc :db/id board-tempid))]
    (-> conn
      (client/transact {:tx-data (cons board-datoms other-datoms)})
      (get-in [:tempids board-tempid]))))

(defn create
  [{:keys [conn] :as request}]
  (let [game-key {:game/ident (UUID/randomUUID)}]
    (board-entities conn
      {board-tempid {:board/ident-string board-api/initial
                     :board/ident-218 board-ident/initial-board-218}}
    (ring.util.response/response game-key))))


(comment
  #:move{:piece-moved :piecename/red-cannon,
         :start-location {:disposition/location :location/h3,
                          :disposition/piece :piecename/red-cannon,
                          :location/index 72,
                          :location/x 7,
                          :location/y 2},
         :end-location {:disposition/location :location/h10,
                        :disposition/piece :piecename/black-horse,
                        :location/index 79,
                        :locatjion/x 7,
                        :location/y 9},
         :piece-taken :piecename/black-horse,
         :board-after "6qzEQWjdQUt25HqXqMiUSY8Yu63ru31LbeJoc",
         :board-before "pgHgrPgmHQSuzVg9ogvpnhxv5CppZ3D5RbxSK"})

(defn game-mostrecent
  [db game-ref]
  (client/pull db
    '[{:game/last-move [:db/id :move/ordinal]
       :game/board [:board/ident-string :db/id]}
      :game/ident
      :db/id]
    game-ref))

;(def tx-tempid-move "temp-moveid")

(defn move-for-tx
  [move-in {:game/keys [last-move]} game-ref]
  (medley/assoc-some
    {:move/piece-moved (:move/piece-moved move-in)
     :move/start-location (get-in move-in [:move/start-location :disposition/location])
     :move/end-location (get-in move-in [:move/end-location :disposition/location])
     :move/ordinal (inc (:move/ordinal last-move 0))
     :game/_moves game-ref}
    :move/prev (:db/id last-move)
    :move/piece-taken (:move/piece-taken move-in)))

(defn gameident-lookupref
  [{:game/keys [ident]}]
  [:game/ident ident])

(defn place-move
  [{:keys [conn db json-params] :as request}]
  (let [game-ref (gameident-lookupref json-params)

        {:move/keys [board-before board-after] :as move}
        (:game/place-move json-params)

        game-recent (game-mostrecent db game-ref)

        {eid-before "board-before" eid-after "board-after"}
        (board-entities conn
          {"board-before" {:board/ident-string board-before}
           "board-after" {:board/ident-string board-after}})]

    (client/transact conn
      {:tx-data [[:db.fn/cas game-ref :game/board eid-before eid-after]
                 (move-for-tx move game-recent game-ref)]})))

(def datomic-client
  (datomic.client.api/client
    {:server-type :peer-server
     :access-key "myaccesskey"
     :secret "mysecret"
     :endpoint "localhost:8998"
     :validate-hostnames false}))

(comment
  (def datomic-game-conn
    (datomic.client.api/connect datomic-client {:db-name "game"}))
  (def dgc datomic-game-conn))
