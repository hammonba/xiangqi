(ns xiangqi.datomic.tx-moves
  (:require [datomic.client.api :as dca]
            [medley.core :as medley]
            [xiangqi.board-utils :as boardutils]
            [xiangqi.utils :as utils]
            [xiangqi.board.board-ident :as board-ident
             :only [decode-boardident-218 ordered-locs]]
            [xiangqi.board.movecalc :as movecalc
             :only [compute-and-generate-next-moves]]))

(defn boardident218-lookupref
  [ident]
  (when ident
    [:board/ident-218 ident]))

(defn prep-move
  [mv]
  (-> mv
    (update :move/start-location :disposition/location)
    (update :move/end-location :disposition/location)
    (update :move/start-board boardident218-lookupref)
    (medley/update-existing :move/end-board boardident218-lookupref)))

(defn tx-endboard-lookuprefs
  "make sure that lookup-refs have been created for all board idents"
  [moves]
  (vec (into #{}
         (comp (map :move/end-board)
           (map (comp #(into {} %) vector boardident218-lookupref)))
         (flatten moves))))

(defn tx-startboard-movescomputed?
  [start-board]
  [:db/cas (boardident218-lookupref start-board)  :board/moves-computed? nil true])

(defn aggregate-moves
  [[mv & _ :as mvs]]
  (assoc (select-keys (prep-move mv)
           [:move/piece-moved :move/start-location :move/start-board])
    :move/aggregation (map-indexed
                        (fn [idx mv]
                            (assoc (prep-move mv) :move/ordering idx)) mvs)))
(defn detect-winner-loser
  "if all the moves are illegal, then player has lost"
  [{:board/keys [player ident]} moves]
  (if (every? :move/illegal? (flatten moves))
    {:board/ident-218 ident
     :board/winner (boardutils/opponent player)
     :board/loser player}
    {}))

(defn build-move-txes
  [{:board/keys [ident] :as startboard} moves]
  [(tx-endboard-lookuprefs moves)
   (into [(tx-startboard-movescomputed? ident)
          (detect-winner-loser startboard moves)]
     (comp cat (map aggregate-moves)) moves)])

(defn transact-tx-matrix
  "we have a matrix of txes; transact them in order"
  [conn txes-coll]
  (into []
    (comp
      (map #(assoc {} :tx-data %))
      (map #(dca/transact conn %)))
    txes-coll))

(defn board-nextmove-txes
  [boardident-218]
  (let [board (board-ident/decode-boardident-218 boardident-218)]
    (->> board
      movecalc/compute-and-generate-next-moves
      (build-move-txes board))))

(defn transact-next-moves
  [conn boardident-218]
  (->>
    (board-nextmove-txes boardident-218)
    (transact-tx-matrix conn)))

#_(defn query-board
  [db board-ident]
  (dca/pull db '[*
                 {:board/winner [:db/ident]}
                 {:board/loser [:db/ident]}]
    (boardident218-lookupref board-ident)))

(defn query-moves-computed?
  [db board-ident]
  (dca/q {:query
          '[:find ?e ?moves
            :in $ ?board-ident
            :where
            [?e :board/ident-218 ?board-ident]
            [?e :board/moves-computed? ?moves]]
          :args [db board-ident]}))

(defn query-moves-for-board
  [db board-eid]
  (dca/q {:query
          '[:find (pull ?mv [{:move/piece-moved [:db/ident]
                              :move/start-location [:db/ident :location/x :location/y]
                              :move/aggregation [:move/ordering
                                                 :move/illegal?
                                                 :move/flying-general?
                                                 {:move/checked-by [:db/ident :location/x :location/y]}
                                                 {:move/end-board [:board/ident-218]}
                                                 {:move/end-location [:db/ident :location/x :location/y]}
                                                 {:move/piece-taken [:db/ident]}]}])
            :in $ ?sb
            :where [?mv :move/start-board ?sb] [?mv :move/aggregation]]
          :args [db board-eid]}))

(defn creating-query-moves-for-board
  [conn board-ident]
  (let [db (dca/db conn)
        [[eid moves?]] (query-moves-computed? db board-ident)]
    (if moves?
      (query-moves-for-board db eid)
      (do
        (transact-next-moves conn board-ident)
        (query-moves-for-board
          (dca/db conn)
          (boardident218-lookupref board-ident))))))

#_(defn collapse-with
  "we get alot of single-entry :db/ident maps. collapse them here"
  [mm f ks]
  (persistent!
    (reduce
      (fn [mm k] (if-let [v (f (get mm k))]
                   (assoc! mm k v)
                   mm))
      (transient mm)
      ks)))

#_(defn transform-movemap
  [mm]
  (-> mm
    (collapse-with :db/ident [:move/piece-moved :move/piece-taken ])
    (collapse-with :board/ident-218 [:move/start-board :move/end-board])

    (medley/update-existing :move/aggregation
      (fn [agg] (when agg
                  (->> agg
                    (sort-by :move/ordering)
                    (mapv (comp #(dissoc % :move/ordering) transform-movemap))))))))

#_(defn query-startlocs-for-board
  [db board-ident]
  (dca/q {:query
          '[:find (pull ?sb [{:move/start-location [:db/ident :location/x :location/y]}])
            :in $
            :where [?sb :move/start-board] [?sb :move/aggregation]]
          :args [db board-ident]}))

#_(defn transform-startlocs
  [startlocs]
  (into #{}
    (comp cat
      (map :move/start-location)
      (map #(set/rename-keys % {:db/ident :location}))
      (map utils/strip-all-namespaces))
    startlocs))

#_(defn query-and-transform-startlocs
  [db board-ident]
  (transform-startlocs (query-startlocs-for-board db board-ident)))

(defn simplify-aggmove
  [{:move/keys [start-location start-board aggregation piece-moved]}]
  (let [end-locs
        (keep
          (fn [loc]
              (when-not (:move/illegal? loc)
                {:end-location (get-in loc [:move/end-location :db/ident])
                 :next-board (get-in loc [:move/end-board :board/ident-218])
                 :location/x (get-in loc [:move/end-location :location/x])
                 :location/y (get-in loc [:move/end-location :location/y])}))
          aggregation)]
    (when-not (empty? end-locs)
      {:piece-moved (:db/ident piece-moved)
       :start-location (:db/ident start-location)
       :end-locations end-locs})))

(defn addmoves-to-dispvec
  [disp-vec ident moves opened-move]
  (transduce
    (comp
      cat
      (keep simplify-aggmove))
    (completing
      (fn [m {:keys [start-location] :as v}]
          (-> m
            (assoc-in [start-location :board/ident] ident)
            (update-in [start-location :end-locations] utils/conj-vec v)))
      (fn [m] (mapv m board-ident/ordered-locs)))
    (cond-> (utils/zip-by :disposition/location disp-vec)
      opened-move (update opened-move assoc :opened-move true))
    moves))

(defn addmoves-to-board
  [{:keys [conn opened-move] :board/keys [ident] :as board}]
  (update board
    :board/disp-vec
    addmoves-to-dispvec
    ident
    (creating-query-moves-for-board conn ident)
    opened-move))