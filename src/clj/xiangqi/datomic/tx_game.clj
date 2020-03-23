(ns xiangqi.datomic.tx-game
  (:import [java.util UUID]))

(defn create-game
  []
  {:game/uuid (UUID/randomUUID)})


(defn apply-move-to-game
  [game-uuid move-counter next-board]
  [
   [:db.fn/cas [:game/uuid game-uuid] :game/move-counter move-counter (inc move-counter)]])
