(ns xiangqi.service.game-service)

(defn create-game
  "create a new game.
   returns game identifier.
   the game will still have to be joined"
  []
  )

(defn join-game
  "request permission to advance moves in the game
   returns a cookie that will need to be submitted
   in future moves"
  [user-id game-id colour]
  )

(defn leave-game
  "invalidates the cookie from this game"
  [cookie]
  )

(defn apply-move-to-game
  "If the cookie checks out as someone permitted to
   make a move on this game on this colour
   and the board ident is a legal move then apply.
   Returns an updated game"
  [cookie game-id board-ident]
  )

(defn resync-game
  "return the current status of this game"
  [game-id]
  )

(defn concede-game
  [cookie game-id]
  "current player admits defeat in an untimely manner")

(defn watch-game
  "subscribe to updates to this game"
  [user-id game-id])

(defn unwatch-game
  "unsubscribe from game updates"
  [user-id game-id])

(defn my-games
  "returns game-ids for all games in which I am involved"
  [user-id unfinished? limit])

(defn all-games
  "returns game-ids for (most recent) games from everyone")
