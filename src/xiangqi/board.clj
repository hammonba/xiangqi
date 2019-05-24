(ns xiangqi.board
	(:require [datomic.client.api :as dca]
						[medley.core :as medley]
						[xiangqi.datomic.peer]))

(defn boardmap-from-db
	[conn]
	(into {}
		(comp
			(mapcat identity)
			(map (juxt :db/ident #(-> %
															(update :location/player :db/ident)
															(update :location/north :db/ident)
															(update :location/south :db/ident)
															(update :location/east :db/ident)
															(update :location/west :db/ident)
															(assoc :location/coords [(:location/x %) (:location/y %)])))))
		(dca/q {:query '[:find (pull ?e [:db/ident
																		 {:location/player [:db/ident]}
																		 :location/palace
																		 :location/x
																		 :location/y
																		 {:location/north [:db/ident]}
																		 {:location/south [:db/ident]}
																		 {:location/east [:db/ident]}
																		 {:location/west [:db/ident]}
																		 ])
										 :in $
										 :where [?e :location/player]]
						:args [(dca/db conn)]})))

(def boardmap
	'#:location{:g6 {:location/y 6,
									 :location/x 6,
									 :location/north :location/h6,
									 :location/coords [6 6],
									 :location/player :player/red,
									 :location/west :location/g7,
									 :location/south :location/f6,
									 :db/ident :location/g6,
									 :location/east :location/g5},
							:d1 {:location/y 3,
									 :location/x 1,
									 :location/north :location/e1,
									 :location/coords [1 3],
									 :location/player :player/black,
									 :location/west :location/d2,
									 :location/south :location/c1,
									 :db/ident :location/d1,
									 :location/east :location/d0},
							:a2 {:location/y 0,
									 :location/x 2,
									 :location/north :location/b2,
									 :location/coords [2 0],
									 :location/player :player/black,
									 :location/west :location/a3,
									 :location/south nil,
									 :db/ident :location/a2,
									 :location/east :location/a1},
							:j8 {:location/y 9,
									 :location/x 8,
									 :location/north nil,
									 :location/coords [8 9],
									 :location/player :player/red,
									 :location/west nil,
									 :location/south :location/i8,
									 :db/ident :location/j8,
									 :location/east :location/h7},
							:e1 {:location/y 4,
									 :location/x 1,
									 :location/north :location/f1,
									 :location/coords [1 4],
									 :location/player :player/black,
									 :location/west :location/e2,
									 :location/south :location/d1,
									 :db/ident :location/e1,
									 :location/east :location/e0},
							:b2 {:location/y 1,
									 :location/x 2,
									 :location/north :location/c2,
									 :location/coords [2 1],
									 :location/player :player/black,
									 :location/west :location/c3,
									 :location/south :location/a2,
									 :db/ident :location/b2,
									 :location/east :location/c1},
							:d2 {:location/y 3,
									 :location/x 2,
									 :location/north :location/e2,
									 :location/coords [2 3],
									 :location/player :player/black,
									 :location/west :location/d3,
									 :location/south :location/c2,
									 :db/ident :location/d2,
									 :location/east :location/d1},
							:f8 {:location/y 5,
									 :location/x 8,
									 :location/north :location/g8,
									 :location/coords [8 5],
									 :location/player :player/red,
									 :location/west nil,
									 :location/south :location/e8,
									 :db/ident :location/f8,
									 :location/east :location/f7},
							:j3 {:location/y 9,
									 :location/x 3,
									 :location/north nil,
									 :location/coords [3 9],
									 :location/player :player/red,
									 :location/west :location/h4,
									 :location/south :location/i3,
									 :db/ident :location/j3,
									 :location/palace true,
									 :location/east :location/h2},
							:i4 {:location/y 8,
									 :location/x 4,
									 :location/north :location/j4,
									 :location/coords [4 8],
									 :location/player :player/red,
									 :location/west :location/i5,
									 :location/south :location/h4,
									 :db/ident :location/i4,
									 :location/palace true,
									 :location/east :location/i3},
							:f1 {:location/y 5,
									 :location/x 1,
									 :location/north :location/g1,
									 :location/coords [1 5],
									 :location/player :player/red,
									 :location/west :location/f2,
									 :location/south :location/e1,
									 :db/ident :location/f1,
									 :location/east :location/f0},
							:h8 {:location/y 7,
									 :location/x 8,
									 :location/north :location/i8,
									 :location/coords [8 7],
									 :location/player :player/red,
									 :location/west nil,
									 :location/south :location/g8,
									 :db/ident :location/h8,
									 :location/east :location/h7},
							:d6 {:location/y 3,
									 :location/x 6,
									 :location/north :location/e6,
									 :location/coords [6 3],
									 :location/player :player/black,
									 :location/west :location/d7,
									 :location/south :location/c6,
									 :db/ident :location/d6,
									 :location/east :location/d5},
							:i8 {:location/y 8,
									 :location/x 8,
									 :location/north :location/j8,
									 :location/coords [8 8],
									 :location/player :player/red,
									 :location/west nil,
									 :location/south :location/h8,
									 :db/ident :location/i8,
									 :location/east :location/i7},
							:d0 {:location/y 3,
									 :location/x 0,
									 :location/north :location/e0,
									 :location/coords [0 3],
									 :location/player :player/black,
									 :location/west :location/d1,
									 :location/south :location/c0,
									 :db/ident :location/d0,
									 :location/east nil},
							:c3 {:location/y 2,
									 :location/x 3,
									 :location/north :location/b3,
									 :location/coords [3 2],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c4,
									 :location/south :location/c3,
									 :db/ident :location/c3,
									 :location/east :location/b2},
							:i5 {:location/y 8,
									 :location/x 5,
									 :location/north :location/j5,
									 :location/coords [5 8],
									 :location/player :player/red,
									 :location/west :location/i6,
									 :location/south :location/h5,
									 :db/ident :location/i5,
									 :location/palace true,
									 :location/east :location/i4},
							:j7 {:location/y 9,
									 :location/x 7,
									 :location/north nil,
									 :location/coords [7 9],
									 :location/player :player/red,
									 :location/west :location/h8,
									 :location/south :location/i7,
									 :db/ident :location/j7,
									 :location/east :location/h6},
							:d5 {:location/y 3,
									 :location/x 5,
									 :location/north :location/e5,
									 :location/coords [5 3],
									 :location/player :player/black,
									 :location/west :location/d6,
									 :location/south :location/c5,
									 :db/ident :location/d5,
									 :location/east :location/d4},
							:h7 {:location/y 7,
									 :location/x 7,
									 :location/north :location/i7,
									 :location/coords [7 7],
									 :location/player :player/red,
									 :location/west :location/h8,
									 :location/south :location/g7,
									 :db/ident :location/h7,
									 :location/east :location/h6},
							:e2 {:location/y 4,
									 :location/x 2,
									 :location/north :location/f2,
									 :location/coords [2 4],
									 :location/player :player/black,
									 :location/west :location/e3,
									 :location/south :location/d2,
									 :db/ident :location/e2,
									 :location/east :location/e1},
							:c6 {:location/y 2,
									 :location/x 6,
									 :location/north :location/d6,
									 :location/coords [6 2],
									 :location/player :player/black,
									 :location/west :location/c7,
									 :location/south :location/b6,
									 :db/ident :location/c6,
									 :location/east :location/c5},
							:e3 {:location/y 4,
									 :location/x 3,
									 :location/north :location/f3,
									 :location/coords [3 4],
									 :location/player :player/black,
									 :location/west :location/e4,
									 :location/south :location/d3,
									 :db/ident :location/e3,
									 :location/east :location/e2},
							:i0 {:location/y 8,
									 :location/x 0,
									 :location/north :location/j0,
									 :location/coords [0 8],
									 :location/player :player/red,
									 :location/west :location/i1,
									 :location/south :location/h0,
									 :db/ident :location/i0,
									 :location/east nil},
							:a3 {:location/y 0,
									 :location/x 3,
									 :location/north :location/b3,
									 :location/coords [3 0],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/a4,
									 :location/south nil,
									 :db/ident :location/a3,
									 :location/east :location/a2},
							:a1 {:location/y 0,
									 :location/x 1,
									 :location/north :location/b1,
									 :location/coords [1 0],
									 :location/player :player/black,
									 :location/west :location/a2,
									 :location/south nil,
									 :db/ident :location/a1,
									 :location/east :location/a0},
							:g0 {:location/y 6,
									 :location/x 0,
									 :location/north :location/h0,
									 :location/coords [0 6],
									 :location/player :player/red,
									 :location/west :location/g1,
									 :location/south :location/f0,
									 :db/ident :location/g0,
									 :location/east nil},
							:i7 {:location/y 8,
									 :location/x 7,
									 :location/north :location/j7,
									 :location/coords [7 8],
									 :location/player :player/red,
									 :location/west :location/i8,
									 :location/south :location/h7,
									 :db/ident :location/i7,
									 :location/east :location/i6},
							:e8 {:location/y 4,
									 :location/x 8,
									 :location/north :location/f8,
									 :location/coords [8 4],
									 :location/player :player/black,
									 :location/west nil,
									 :location/south :location/d8,
									 :db/ident :location/e8,
									 :location/east :location/e7},
							:b1 {:location/y 1,
									 :location/x 1,
									 :location/north :location/c1,
									 :location/coords [1 1],
									 :location/player :player/black,
									 :location/west :location/c2,
									 :location/south :location/a1,
									 :db/ident :location/b1,
									 :location/east :location/c0},
							:c4 {:location/y 2,
									 :location/x 4,
									 :location/north :location/b4,
									 :location/coords [4 2],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c5,
									 :location/south :location/c4,
									 :db/ident :location/c4,
									 :location/east :location/b3},
							:g4 {:location/y 6,
									 :location/x 4,
									 :location/north :location/h4,
									 :location/coords [4 6],
									 :location/player :player/red,
									 :location/west :location/g5,
									 :location/south :location/f4,
									 :db/ident :location/g4,
									 :location/east :location/g3},
							:g2 {:location/y 6,
									 :location/x 2,
									 :location/north :location/h2,
									 :location/coords [2 6],
									 :location/player :player/red,
									 :location/west :location/g3,
									 :location/south :location/f2,
									 :db/ident :location/g2,
									 :location/east :location/g1},
							:f5 {:location/y 5,
									 :location/x 5,
									 :location/north :location/g5,
									 :location/coords [5 5],
									 :location/player :player/red,
									 :location/west :location/f6,
									 :location/south :location/e5,
									 :db/ident :location/f5,
									 :location/east :location/f4},
							:e6 {:location/y 4,
									 :location/x 6,
									 :location/north :location/f6,
									 :location/coords [6 4],
									 :location/player :player/black,
									 :location/west :location/e7,
									 :location/south :location/d6,
									 :db/ident :location/e6,
									 :location/east :location/e5},
							:b8 {:location/y 1,
									 :location/x 8,
									 :location/north :location/c8,
									 :location/coords [8 1],
									 :location/player :player/black,
									 :location/west nil,
									 :location/south :location/a8,
									 :db/ident :location/b8,
									 :location/east :location/c7},
							:d4 {:location/y 3,
									 :location/x 4,
									 :location/north :location/e4,
									 :location/coords [4 3],
									 :location/player :player/black,
									 :location/west :location/d5,
									 :location/south :location/c4,
									 :db/ident :location/d4,
									 :location/east :location/d3},
							:c2 {:location/y 2,
									 :location/x 2,
									 :location/north :location/d2,
									 :location/coords [2 2],
									 :location/player :player/black,
									 :location/west :location/c3,
									 :location/south :location/b2,
									 :db/ident :location/c2,
									 :location/east :location/c1},
							:a8 {:location/y 0,
									 :location/x 8,
									 :location/north :location/b8,
									 :location/coords [8 0],
									 :location/player :player/black,
									 :location/west nil,
									 :location/south nil,
									 :db/ident :location/a8,
									 :location/east :location/a7},
							:h5 {:location/y 7,
									 :location/x 5,
									 :location/north :location/i5,
									 :location/coords [5 7],
									 :location/player :player/red,
									 :location/west :location/h6,
									 :location/south :location/g5,
									 :db/ident :location/h5,
									 :location/palace true,
									 :location/east :location/h4},
							:b0 {:location/y 1,
									 :location/x 0,
									 :location/north :location/c0,
									 :location/coords [0 1],
									 :location/player :player/black,
									 :location/west :location/c1,
									 :location/south :location/a0,
									 :db/ident :location/b0,
									 :location/east nil},
							:i6 {:location/y 8,
									 :location/x 6,
									 :location/north :location/j6,
									 :location/coords [6 8],
									 :location/player :player/red,
									 :location/west :location/i7,
									 :location/south :location/h6,
									 :db/ident :location/i6,
									 :location/east :location/i5},
							:d8 {:location/y 3,
									 :location/x 8,
									 :location/north :location/e8,
									 :location/coords [8 3],
									 :location/player :player/black,
									 :location/west nil,
									 :location/south :location/c8,
									 :db/ident :location/d8,
									 :location/east :location/d7},
							:j4 {:location/y 9,
									 :location/x 4,
									 :location/north nil,
									 :location/coords [4 9],
									 :location/player :player/red,
									 :location/west :location/h5,
									 :location/south :location/i4,
									 :db/ident :location/j4,
									 :location/palace true,
									 :location/east :location/h3},
							:i3 {:location/y 8,
									 :location/x 3,
									 :location/north :location/j3,
									 :location/coords [3 8],
									 :location/player :player/red,
									 :location/west :location/i4,
									 :location/south :location/h3,
									 :db/ident :location/i3,
									 :location/palace true,
									 :location/east :location/i2},
							:a6 {:location/y 0,
									 :location/x 6,
									 :location/north :location/b6,
									 :location/coords [6 0],
									 :location/player :player/black,
									 :location/west :location/a7,
									 :location/south nil,
									 :db/ident :location/a6,
									 :location/east :location/a5},
							:g3 {:location/y 6,
									 :location/x 3,
									 :location/north :location/h3,
									 :location/coords [3 6],
									 :location/player :player/red,
									 :location/west :location/g4,
									 :location/south :location/f3,
									 :db/ident :location/g3,
									 :location/east :location/g2},
							:c0 {:location/y 2,
									 :location/x 0,
									 :location/north :location/d0,
									 :location/coords [0 2],
									 :location/player :player/black,
									 :location/west :location/c1,
									 :location/south :location/b0,
									 :db/ident :location/c0,
									 :location/east nil},
							:g8 {:location/y 6,
									 :location/x 8,
									 :location/north :location/h8,
									 :location/coords [8 6],
									 :location/player :player/red,
									 :location/west nil,
									 :location/south :location/f8,
									 :db/ident :location/g8,
									 :location/east :location/g7},
							:h4 {:location/y 7,
									 :location/x 4,
									 :location/north :location/i4,
									 :location/coords [4 7],
									 :location/player :player/red,
									 :location/west :location/h5,
									 :location/south :location/g4,
									 :db/ident :location/h4,
									 :location/palace true,
									 :location/east :location/h3},
							:g1 {:location/y 6,
									 :location/x 1,
									 :location/north :location/h1,
									 :location/coords [1 6],
									 :location/player :player/red,
									 :location/west :location/g2,
									 :location/south :location/f1,
									 :db/ident :location/g1,
									 :location/east :location/g0},
							:e4 {:location/y 4,
									 :location/x 4,
									 :location/north :location/f4,
									 :location/coords [4 4],
									 :location/player :player/black,
									 :location/west :location/e5,
									 :location/south :location/d4,
									 :db/ident :location/e4,
									 :location/east :location/e3},
							:a0 {:location/y 0,
									 :location/x 0,
									 :location/north :location/b0,
									 :location/coords [0 0],
									 :location/player :player/black,
									 :location/west :location/a1,
									 :location/south nil,
									 :db/ident :location/a0,
									 :location/east nil},
							:c1 {:location/y 2,
									 :location/x 1,
									 :location/north :location/d1,
									 :location/coords [1 2],
									 :location/player :player/black,
									 :location/west :location/c2,
									 :location/south :location/b1,
									 :db/ident :location/c1,
									 :location/east :location/c0},
							:c8 {:location/y 2,
									 :location/x 8,
									 :location/north :location/d8,
									 :location/coords [8 2],
									 :location/player :player/black,
									 :location/west nil,
									 :location/south :location/b8,
									 :db/ident :location/c8,
									 :location/east :location/c7},
							:f3 {:location/y 5,
									 :location/x 3,
									 :location/north :location/g3,
									 :location/coords [3 5],
									 :location/player :player/red,
									 :location/west :location/f4,
									 :location/south :location/e3,
									 :db/ident :location/f3,
									 :location/east :location/f2},
							:b7 {:location/y 1,
									 :location/x 7,
									 :location/north :location/c7,
									 :location/coords [7 1],
									 :location/player :player/black,
									 :location/west :location/c8,
									 :location/south :location/a7,
									 :db/ident :location/b7,
									 :location/east :location/c6},
							:e0 {:location/y 4,
									 :location/x 0,
									 :location/north :location/f0,
									 :location/coords [0 4],
									 :location/player :player/black,
									 :location/west :location/d1,
									 :location/south :location/d0,
									 :db/ident :location/e0,
									 :location/east nil},
							:h6 {:location/y 7,
									 :location/x 6,
									 :location/north :location/i6,
									 :location/coords [6 7],
									 :location/player :player/red,
									 :location/west :location/h7,
									 :location/south :location/g6,
									 :db/ident :location/h6,
									 :location/east :location/h5},
							:b4 {:location/y 1,
									 :location/x 4,
									 :location/north :location/b4,
									 :location/coords [4 1],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c5,
									 :location/south :location/a4,
									 :db/ident :location/b4,
									 :location/east :location/c3},
							:f2 {:location/y 5,
									 :location/x 2,
									 :location/north :location/g2,
									 :location/coords [2 5],
									 :location/player :player/red,
									 :location/west :location/f3,
									 :location/south :location/e2,
									 :db/ident :location/f2,
									 :location/east :location/f1},
							:e5 {:location/y 4,
									 :location/x 5,
									 :location/north :location/f5,
									 :location/coords [5 4],
									 :location/player :player/black,
									 :location/west :location/e6,
									 :location/south :location/d5,
									 :db/ident :location/e5,
									 :location/east :location/e4},
							:f0 {:location/y 5,
									 :location/x 0,
									 :location/north :location/g0,
									 :location/coords [0 5],
									 :location/player :player/red,
									 :location/west :location/f1,
									 :location/south :location/e0,
									 :db/ident :location/f0,
									 :location/east nil},
							:h0 {:location/y 7,
									 :location/x 0,
									 :location/north :location/i0,
									 :location/coords [0 7],
									 :location/player :player/red,
									 :location/west :location/h1,
									 :location/south :location/g0,
									 :db/ident :location/h0,
									 :location/east nil},
							:a5 {:location/y 0,
									 :location/x 5,
									 :location/north :location/b5,
									 :location/coords [5 0],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/a6,
									 :location/south nil,
									 :db/ident :location/a5,
									 :location/east :location/a4},
							:b3 {:location/y 1,
									 :location/x 3,
									 :location/north :location/b3,
									 :location/coords [3 1],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c4,
									 :location/south :location/a3,
									 :db/ident :location/b3,
									 :location/east :location/c2},
							:c5 {:location/y 2,
									 :location/x 5,
									 :location/north :location/b5,
									 :location/coords [5 2],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c6,
									 :location/south :location/c5,
									 :db/ident :location/c5,
									 :location/east :location/b4},
							:d7 {:location/y 3,
									 :location/x 7,
									 :location/north :location/e7,
									 :location/coords [7 3],
									 :location/player :player/black,
									 :location/west :location/d8,
									 :location/south :location/c7,
									 :db/ident :location/d7,
									 :location/east :location/d6},
							:b6 {:location/y 1,
									 :location/x 6,
									 :location/north :location/c6,
									 :location/coords [6 1],
									 :location/player :player/black,
									 :location/west :location/c7,
									 :location/south :location/a6,
									 :db/ident :location/b6,
									 :location/east :location/c5},
							:g7 {:location/y 6,
									 :location/x 7,
									 :location/north :location/h7,
									 :location/coords [7 6],
									 :location/player :player/red,
									 :location/west :location/g8,
									 :location/south :location/f7,
									 :db/ident :location/g7,
									 :location/east :location/g6},
							:h2 {:location/y 7,
									 :location/x 2,
									 :location/north :location/i2,
									 :location/coords [2 7],
									 :location/player :player/red,
									 :location/west :location/h3,
									 :location/south :location/g2,
									 :db/ident :location/h2,
									 :location/east :location/h1},
							:i2 {:location/y 8,
									 :location/x 2,
									 :location/north :location/j2,
									 :location/coords [2 8],
									 :location/player :player/red,
									 :location/west :location/i3,
									 :location/south :location/h2,
									 :db/ident :location/i2,
									 :location/east :location/i1},
							:f7 {:location/y 5,
									 :location/x 7,
									 :location/north :location/g7,
									 :location/coords [7 5],
									 :location/player :player/red,
									 :location/west :location/f8,
									 :location/south :location/e7,
									 :db/ident :location/f7,
									 :location/east :location/f6},
							:j1 {:location/y 9,
									 :location/x 1,
									 :location/north nil,
									 :location/coords [1 9],
									 :location/player :player/red,
									 :location/west :location/h2,
									 :location/south :location/i1,
									 :db/ident :location/j1,
									 :location/east :location/h0},
							:h1 {:location/y 7,
									 :location/x 1,
									 :location/north :location/i1,
									 :location/coords [1 7],
									 :location/player :player/red,
									 :location/west :location/h2,
									 :location/south :location/g1,
									 :db/ident :location/h1,
									 :location/east :location/h0},
							:d3 {:location/y 3,
									 :location/x 3,
									 :location/north :location/e3,
									 :location/coords [3 3],
									 :location/player :player/black,
									 :location/west :location/d4,
									 :location/south :location/c3,
									 :db/ident :location/d3,
									 :location/east :location/d2},
							:i1 {:location/y 8,
									 :location/x 1,
									 :location/north :location/j1,
									 :location/coords [1 8],
									 :location/player :player/red,
									 :location/west :location/i2,
									 :location/south :location/h1,
									 :db/ident :location/i1,
									 :location/east :location/i0},
							:h3 {:location/y 7,
									 :location/x 3,
									 :location/north :location/i3,
									 :location/coords [3 7],
									 :location/player :player/red,
									 :location/west :location/h4,
									 :location/south :location/g3,
									 :db/ident :location/h3,
									 :location/palace true,
									 :location/east :location/h2},
							:a7 {:location/y 0,
									 :location/x 7,
									 :location/north :location/b7,
									 :location/coords [7 0],
									 :location/player :player/black,
									 :location/west :location/a8,
									 :location/south nil,
									 :db/ident :location/a7,
									 :location/east :location/a6},
							:j5 {:location/y 9,
									 :location/x 5,
									 :location/north nil,
									 :location/coords [5 9],
									 :location/player :player/red,
									 :location/west :location/h6,
									 :location/south :location/i5,
									 :db/ident :location/j5,
									 :location/palace true,
									 :location/east :location/h4},
							:j6 {:location/y 9,
									 :location/x 6,
									 :location/north nil,
									 :location/coords [6 9],
									 :location/player :player/red,
									 :location/west :location/h7,
									 :location/south :location/i6,
									 :db/ident :location/j6,
									 :location/east :location/h5},
							:f6 {:location/y 5,
									 :location/x 6,
									 :location/north :location/g6,
									 :location/coords [6 5],
									 :location/player :player/red,
									 :location/west :location/f7,
									 :location/south :location/e6,
									 :db/ident :location/f6,
									 :location/east :location/f5},
							:c7 {:location/y 2,
									 :location/x 7,
									 :location/north :location/d7,
									 :location/coords [7 2],
									 :location/player :player/black,
									 :location/west :location/c8,
									 :location/south :location/b7,
									 :db/ident :location/c7,
									 :location/east :location/c6},
							:f4 {:location/y 5,
									 :location/x 4,
									 :location/north :location/g4,
									 :location/coords [4 5],
									 :location/player :player/red,
									 :location/west :location/f5,
									 :location/south :location/e4,
									 :db/ident :location/f4,
									 :location/east :location/f3},
							:j2 {:location/y 9,
									 :location/x 2,
									 :location/north nil,
									 :location/coords [2 9],
									 :location/player :player/red,
									 :location/west :location/h3,
									 :location/south :location/i2,
									 :db/ident :location/j2,
									 :location/east :location/h1},
							:b5 {:location/y 1,
									 :location/x 5,
									 :location/north :location/b5,
									 :location/coords [5 1],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/c6,
									 :location/south :location/a5,
									 :db/ident :location/b5,
									 :location/east :location/c4},
							:g5 {:location/y 6,
									 :location/x 5,
									 :location/north :location/h5,
									 :location/coords [5 6],
									 :location/player :player/red,
									 :location/west :location/g6,
									 :location/south :location/f5,
									 :db/ident :location/g5,
									 :location/east :location/g4},
							:e7 {:location/y 4,
									 :location/x 7,
									 :location/north :location/f7,
									 :location/coords [7 4],
									 :location/player :player/black,
									 :location/west :location/e8,
									 :location/south :location/d7,
									 :db/ident :location/e7,
									 :location/east :location/e6},
							:j0 {:location/y 9,
									 :location/x 0,
									 :location/north nil,
									 :location/coords [0 9],
									 :location/player :player/red,
									 :location/west :location/h1,
									 :location/south :location/i0,
									 :db/ident :location/j0,
									 :location/east nil},
							:a4 {:location/y 0,
									 :location/x 4,
									 :location/north :location/b4,
									 :location/coords [4 0],
									 :location/player :player/black,
									 :location/palace true,
									 :location/west :location/a5,
									 :location/south nil,
									 :db/ident :location/a4,
									 :location/east :location/a3}})

(defn build-boardmatrix
	[boardmap]
	(->>
		(vals boardmap)
		(sort-by (juxt :location/x :location/y))
		(partition-by :location/x)
		(mapv vec)))


(def boardmatrix (build-boardmatrix boardmap))

(defn retrieve-disposition
	[db board-eid]
	(dca/pull db
		'[{:board/next-player [:db/ident]}
			{:board/disposition [{:disposition/piece [:db/ident
																								{:piece/type [:db/ident]}
																								{:piece/player [:db/ident]}]}
													 {:disposition/location [:db/ident
																									 :location/x
																									 :location/y
																									 {:location/player [:db/ident]}]}]}]
		board-eid))

(defn collate-disposition
	"return a board matrix with pieces assoc'd into locations"
	[{:board/keys [disposition]}]
	(reduce
		(fn [mtx {:disposition/keys [piece location]}]
				(update-in mtx (get-in boardmap [(:db/ident location) :location/coords]) assoc :disposition/piece piece))
		boardmatrix
		disposition))

(defn disppiece-ident
	[disp-loc]
	(get-in disp-loc [:disposition/piece :db/ident]))

(defn is-disploc-piecetype?
	[piecetype disp-loc]
	(= piecetype (get-in disp-loc [:disposition/piece :piece/type :db/ident])))

(defn is-disploc-pieceowner?
	[player disp-loc]
	(= player (get-in disp-loc [:disposition/piece :piece/player :db/ident])))

(def opponent
	{:player/red :player/black
	 :player/black :player/red})

(defn dispositioned-pieces-for-player
	[player disposition]
	(filter #(is-disploc-pieceowner? player %) disposition))

(def piece-hashnumbers
	{nil 0
	 :piecename/red-soldier 1
	 :piecename/black-private 2
	 :piecename/red-cannon 3
	 :piecename/black-catapult 4
	 :piecename/red-chariot 5
	 :piecename/black-chariot 6
	 :piecename/red-horse 7
	 :piecename/black-horse 8
	 :piecename/red-minister 9
	 :piecename/black-elephant 10
	 :piecename/red-guard 11
	 :piecename/black-guard 12
	 :piecename/red-marshal 13
	 :piecename/black-general 14})

(def seed-board-ident-by-nextplayer
	{:player/red 0N
	 :player/black 1N})

(def ordered-locs
	[nil
	 :location/a0
	 :location/b0
	 :location/c0
	 :location/d0
	 :location/e0
	 :location/f0
	 :location/g0
	 :location/h0
	 :location/i0
	 :location/j0
	 :location/a1
	 :location/b1
	 :location/c1
	 :location/d1
	 :location/e1
	 :location/f1
	 :location/g1
	 :location/h1
	 :location/i1
	 :location/j1
	 :location/a2
	 :location/b2
	 :location/c2
	 :location/d2
	 :location/e2
	 :location/f2
	 :location/g2
	 :location/h2
	 :location/i2
	 :location/j2
	 :location/a3
	 :location/b3
	 :location/c3
	 :location/d3
	 :location/e3
	 :location/f3
	 :location/g3
	 :location/h3
	 :location/i3
	 :location/j3
	 :location/a4
	 :location/b4
	 :location/c4
	 :location/d4
	 :location/e4
	 :location/f4
	 :location/g4
	 :location/h4
	 :location/i4
	 :location/j4
	 :location/a5
	 :location/b5
	 :location/c5
	 :location/d5
	 :location/e5
	 :location/f5
	 :location/g5
	 :location/h5
	 :location/i5
	 :location/j5
	 :location/a6
	 :location/b6
	 :location/c6
	 :location/d6
	 :location/e6
	 :location/f6
	 :location/g6
	 :location/h6
	 :location/i6
	 :location/j6
	 :location/a7
	 :location/b7
	 :location/c7
	 :location/d7
	 :location/e7
	 :location/f7
	 :location/g7
	 :location/h7
	 :location/i7
	 :location/j7
	 :location/a8
	 :location/b8
	 :location/c8
	 :location/d8
	 :location/e8
	 :location/f8
	 :location/g8
	 :location/h8
	 :location/i8
	 :location/j8])

(def blackpalace-locs
	[nil
	 :location/a3
	 :location/b3
	 :location/c3
	 :location/a4
	 :location/b4
	 :location/c4
	 :location/a5
	 :location/b5
	 :location/c5])

(def blackelephant-locs
	[nil
	 :location/a0
	 :location/a2
	 :location/a4
	 :location/a6
	 :location/a8
	 :location/c0
	 :location/c2
	 :location/c4
	 :location/c6
	 :location/c8
	 :location/e0
	 :location/e2
	 :location/e4
	 :location/e6
	 :location/e8])

(def redpalace-locs
	[:location/h3
	 :location/i3
	 :location/j3
	 :location/h4
	 :location/i4
	 :location/j4
	 :location/h5
	 :location/i5
	 :location/j5])

(def redelephant-locs
	[nil
	 :location/f0
	 :location/f2
	 :location/f4
	 :location/f6
	 :location/f8
	 :location/h0
	 :location/h2
	 :location/h4
	 :location/h6
	 :location/h8
	 :location/j0
	 :location/j2
	 :location/j4
	 :location/j6
	 :location/j8])

(defn bitcount
	"how many bits are required to encode n"
	[n]
	(reduce (fn [n idx]
							(if (zero? n)
								(reduced idx)
								(quot n 2)))
		n
		(range)))

(defn locs-to-order
	[loc-vec]
	(let [bitcount (bitcount (count loc-vec))
				bitmask (nth (iterate #(* 2 %) 1) bitcount)]
		(assoc (zipmap loc-vec (range))
			:bitcount bitcount
			:bitmask bitmask)))

(def loc-encoding
	(let [red-palace (locs-to-order redpalace-locs)
				black-palace (locs-to-order blackpalace-locs)]
		{:piecename/red-minister (locs-to-order redelephant-locs)
		 :piecename/red-guard red-palace
		 :piecename/red-marshal red-palace
		 :piecename/black-elephant (locs-to-order blackelephant-locs)
		 :piecename/black-guard black-palace
		 :piecename/black-marshal black-palace
		 :default (locs-to-order ordered-locs)}))

(defn compute-board-ident-361
	"distil the board postion into a single long"
	[next-player collated-disposition]
	(reduce (fn [acc dl]
							(+ (* acc 16)
								(get piece-hashnumbers (get-in dl [:disposition/piece :db/ident]))))
		(seed-board-ident-by-nextplayer next-player)
		(flatten collated-disposition)))
;;361 bits to encode start boarad

(defn compute-board-ident
	[{:board/keys [next-player disposition]}]
	(let [default-loc (:default loc-encoding)]
		(transduce
			(comp (map (juxt
									 #(get-in % [:disposition/piece :db/ident])
									 #(get-in % [:disposition/location :db/ident])))
				(map (fn [[p l]]
								 (let [{:keys [bitmask] :as enc} default-loc]
									 [bitmask (get enc l)]))))
			(fn
			 ([acc] acc)
			 ([acc [mask v]] (+ v (* mask acc))))
			(seed-board-ident-by-nextplayer (:db/ident next-player))
			disposition)))

(def encoding-vec (vec "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(defn bigint-as-string
	"expresses bigint an an alphanumeric string. This gives us uniqueness
  without exposing a raw implementation detail"
	([n] (bigint-as-string encoding-vec n))
	([encoding-vec eid]
	 (let [radix (count encoding-vec)]
		 (transduce
			 (comp (take-while pos?)
				 (map #(rem % radix))
				 (map encoding-vec))
			 (completing #(.append ^StringBuilder %1 ^char %2) str)
			 (StringBuilder.)
			 (iterate #(quot % radix) eid)))))

(def sorted-piece-comparator
	(comparator (fn [x y]
									(- (piece-hashnumbers y) (piece-hashnumbers x)))))

(def sorted-piecemap
	(sorted-map-by sorted-piece-comparator
		:piecename/red-soldier '()
		:piecename/black-private '()
		:piecename/red-cannon '()
		:piecename/black-catapult '()
		:piecename/red-chariot '()
		:piecename/black-chariot '()
		:piecename/red-horse '()
		:piecename/black-horse '()
		:piecename/red-minister '()
		:piecename/black-elephant '()
		:piecename/red-guard '()
		:piecename/black-guard '()
		:piecename/red-marshal '()
		:piecename/black-general '()))


(defn extant-piecemap
	[disposition]
	(reduce
		(fn [m dp]
				(update m
					(get-in dp [:disposition/piece :db/ident])
					conj
					(get-in dp [:disposition/location :db/ident])))
		{:piecename/red-soldier '()
		 :piecename/black-private '()
		 :piecename/red-cannon '()
		 :piecename/black-catapult '()
		 :piecename/red-chariot '()
		 :piecename/black-chariot '()
		 :piecename/red-horse '()
		 :piecename/black-horse '()
		 :piecename/red-minister '()
		 :piecename/black-elephant '()
		 :piecename/red-guard '()
		 :piecename/black-guard '()
		 :piecename/red-marshal '()
		 :piecename/black-general '()}
		disposition))


(defn compute-board-ident3
	[{:board/keys [next-player disposition]}]
	(let [default-loc (:default loc-encoding)]
		(transduce
			(comp (map (juxt
									 #(get-in % [:disposition/piece :db/ident])
									 #(get-in % [:disposition/location :db/ident])))
				(map (fn [[p l]]
								 (println [p l])
								 (let [{:keys [bitmask] :as enc} (get loc-encoding p default-loc)]
									 [bitmask (get enc l)]))))
			(fn
			 ([acc] acc)
			 ([acc [mask v]] (+ v (* mask acc))))
			(seed-board-ident-by-nextplayer (:db/ident next-player))
			disposition)))

;;197 bits to encode start board
;;220 bits to encode start boarad

(defn unpack-board-from-ident2
	[board-ident]
	(take-while (comp pos? first)
		(iterate (fn [[acc _]]
								 [(quot acc 127) (get ordered-locs (mod acc 127))])
			[board-ident nil])))

(defn unpack-board-from-ident
	"generate board position from single long"
	[board-ident]
	(take-while (comp (complement zero?) first)
		(iterate (fn [[acc _]]
								 [(bit-shift-right acc 4) (bit-and acc 15)])
			[board-ident nil])))

(defn get-board
	[board-id]
	(retrieve-disposition (dca/db xiangqi.datomic.peer/conn) board-id))

(defn disposition-piece-owner
	[disp-loc]
	(get-in disp-loc [:disposition/piece :piece/player :db/ident]))

(defn disposition-loc-owner
	[disp-loc]
	(get-in disp-loc [:disposition/location :location/player :db/ident]))

(defn location-owner
	[disp-loc]
	(get-in disp-loc [:location/player :db/ident]))

#_(defn vacant-loc?
		[disp-loc coord]
		(nil? (occupied-loc disp-loc coord)))

(defn player-in-loc?
	[player loc]
	(= player (disposition-piece-owner loc)))

#_(defn player-piece-locs
	[disp-mtx player]
	(filter #(player-in-loc? player %) (flatten disp-mtx)))

#_(defn forward
	[player [x y]]
	(case player :player/red [x (inc y)]
							 :player/black [x (dec y)]))

#_(defn north [[x y]]
	(when (and x y)
		[x (dec y)]))

#_(defn south [[x y]]
	(when (and x y)
		[x (inc y)]))

#_(defn west [[x y]]
	(when (and x y)
		[(inc x) y]))
#_(defn east [[x y]]
	(when (and x y)
		[(dec x) y]))
#_(def northeast (comp north east))
#_(def northwest (comp north west))

#_(def southeast (comp south east))
#_(def southwest (comp south west))
#_(defmulti forward (fn [player coords] player))
#_(defmethod forward :player/red [_ coords] (north coords))


#_(defmethod forward :player/black [_ coords] (south coords))
(defn within-bounds
	[[x y]]
	(and x y (<= 0 x 8) (<= 0 y 10)))
(defn check-bounds
	[coord]
	(when (within-bounds coord)
		coord))


(defmulti movelocs-for-piece
	(fn [disp-mtx piece-disp] (get-in piece-disp [:disposition/piece :piece/type :db/ident])))

(defn piece-owner
	[piece]
	(get-in piece [:piece/player :db/ident]))


(defn generate-moves-for-piece
	"compute individual moves for piece
	take out those that land on one of our piece
	take out those that move us into check
	take out those that enable the antagonists to face each other
	"
	[{:disposition/keys [piece location]} dest-locs]
	(let [us (piece-owner piece)]
		(sequence
			(comp
				(remove (fn [dest-loc]
										(= us (piece-owner (:disposition/piece dest-loc)))))
				(map (fn [dest-loc]
								 (medley/assoc-some
									 {:move/piece-moved piece
										:move/start-location location
										:move/end-location dest-loc}
									 :move/piece-taken (get-in dest-loc [:disposition/piece :db/ident])))))
			dest-locs)))

(defn coords-from-location
	[{:location/keys [x y]}]
	[x y])

(defn apply-move-to-disposition
	[disp-mtx {:move/keys [piece-moved start-location end-location]}]
	(-> disp-mtx
		(update-in (coords-from-location start-location) dissoc :disposition/piece)
		(update-in (coords-from-location end-location) assoc :disposition/piece piece-moved)))


(defn find-general
	[disp-mtx player]
	(->>
		(flatten disp-mtx)
		(filter #(is-disploc-pieceowner? player %))
		(filter #(is-disploc-piecetype? :piecetype/general %))
		(first)))

(defn flatset-of-move-location-idents
	[disp-mtx disp-loc]
	(into #{}
		(map :db/ident)
		(flatten (movelocs-for-piece disp-mtx disp-loc))))

(defn is-move-check?
	"would this move put us into check?"
	[disp-mtx {:move/keys [piece-moved] :as move}]
	(let [disp-mtx2 (apply-move-to-disposition disp-mtx move)
				our-player (piece-owner piece-moved)
				our-general-loc (:db/ident (find-general disp-mtx2 our-player))]
		(reduce
			(fn [_ disp-loc]
					(when (contains?
									(flatset-of-move-location-idents disp-mtx2 disp-loc)
									our-general-loc)
						(reduced disp-loc)))
			nil
			(dispositioned-pieces-for-player (opponent our-player) disp-mtx2))))

(defn are-generals-facing?
	"test whether generals are in the same rank"
	[disp-mtx]
	(let [red-general (find-general disp-mtx :player/red)
				black-general (find-general disp-mtx :player/black)]
		(when (= (:location/x red-general) (:location/x black-general))
			(= :piecename/red-marshal
				(disppiece-ident (first (remove unoccupied-loc? (second (file disp-mtx black-general)))))))))

(defn apply-boardlevel-movetest
	"would this move put us into check?
	 does this move result in two generals directly facing each other?
	 both should be avoided"
	[disp-mtx {:move/keys [piece-moved] :as move}]
	(let [mtx-next (apply-move-to-disposition disp-mtx move)
				is-check? (is-move-check? mtx-next move)
				generals-facing? (are-generals-facing? mtx-next)
				moving-player (piece-owner piece-moved)]
		(medley/assoc-some move
			:creates-check? is-check?
			:generals-facing? (when generals-facing? true)
			:illegal-move? (or (some? is-check?) generals-facing?)
			:move/start-board (compute-board-ident-361 moving-player disp-mtx)
			:move/end-board (compute-board-ident-361 (opponent moving-player) mtx-next))))

(defn compute-and-generate-moves-for-piece
	[disp-mtx piece-disp]
	(map (fn [strand]
					 (map (fn [mv] (apply-boardlevel-movetest disp-mtx mv)) strand))
		(keep
			#(seq (generate-moves-for-piece piece-disp %))
			(movelocs-for-piece disp-mtx piece-disp))))

(defn apply-locfn
	[[x y] [xfn yfn]]
	[(if xfn (xfn x) x)
	 (if yfn (yfn y) y)])

(def advance-fn
	{:player/red inc
	 :player/black dec})

(defmethod movelocs-for-piece :piecetype/soldier
	[disp-mtx disp-loc]
	(let [piece-owner (disposition-piece-owner disp-loc)
				location-owner (disposition-loc-owner disp-loc)
				{:location/keys [x y]} (:disposition/location disp-loc)

				coords (cond-> [[nil (advance-fn piece-owner)]]
								 (not= piece-owner location-owner) (conj [dec nil] [inc nil]))]
		(->> coords
			(mapv #(apply-locfn [x y] %))
			(keep #(get-in disp-mtx %))
			(map list))))

#_(defn segment-board
		[disp-mtx [x y]])

#_(defn next-loc
		[pred-fn disp-mtx move-fn prev-coord]
		(when-let [next-coord (check-bounds (move-fn prev-coord))]
			(if (pred-fn disp-mtx next-coord)
				next-coord
				(recur pred-fn disp-mtx move-fn next-coord))))

#_(defn next-occupied-loc
		[disp-mtx move-fn prev-coord]
		(next-loc occupied-loc disp-mtx move-fn prev-coord))

#_(defn next-vacant-loc
		[disp-mtx move-fn prev-coord]
		(next-loc (comp nil? occupied-loc) disp-mtx move-fn prev-coord))

#_(defn vacant-locs-until
		[disp-mtx move-fn prev-coord]
		(take-while #(and (some? %) (vacant-loc? disp-mtx %))
			(rest (iterate (comp check-bounds move-fn) prev-coord))))

(defn unoccupied-loc?
	[loc]
	(nil? (:disposition/piece loc)))

(def occupied-loc? (complement unoccupied-loc?))

#_(defn find-cannonlocs
		[disp-mtx move-fn prev-coord]
		(let [vac-coords (vec (vacant-locs-until disp-mtx move-fn prev-coord))]
			(conj vac-coords (next-occupied-loc disp-mtx move-fn (move-fn (last vac-coords))))))

#_(defn is-palace?
		[disp-mtx coord]
		(:location/palace (get-in disp-mtx coord)))

#_(def all-dirs
		(juxt north south east west))

#_(def all-diagonals
		(juxt northeast northwest southeast southwest))

(defn reordered-split-at
	"splits the coll at n
	excludes n
	arrange coll so is increasing distance from n"
	[n coll]
	[(reverse (take n coll)) (drop (inc n) coll)])

#_(defn find-idx
		[pred start coll]
		(reduce
			(fn [n v] (if (pred v)
									(reduced n)
									(inc n)))
			start
			(drop start coll)))

(defn filter-cannon-locs
	[loc-coll]
	(let [[f r] (split-with unoccupied-loc? loc-coll)
				t (second (filter occupied-loc? r))]
		(if (nil? t)
			f
			(concat f (list t)))))

(defn single-cross
	"one space in every direction"
	[disp-mtx {:location/keys [x y]}]
	(keep #(get-in disp-mtx %)
		[[(dec x) y] [(inc x) y] [x (dec y)] [x (inc y)]]))

(defn double-diagonals
	"two space in every direction"
	[disp-mtx {:location/keys [x y]}]
	(keep #(get-in disp-mtx %)
		[[(- x 2) (- y 2)] [(+ x 2) (+ y 2)] [(- x 2) (+ y 2)] [(+ x 2) (- y 2)]]))

(defn single-diagonals
	"one space in every diagonal"
	[disp-mtx {:location/keys [x y]}]
	(keep #(get-in disp-mtx %)
		[[(dec x) (dec y)] [(inc x) (inc y)] [(inc x) (dec y)] [(dec x) (inc y)]]))

(defn rank
	"rank is a horizontal row (number)"
	[disp-mtx {:location/keys [x y]}]
	(reordered-split-at x (map #(nth % y) disp-mtx)))

(defn file
	"file is vertical row (letter)"
	[disp-mtx {:location/keys [x y]}]
	(reordered-split-at y (nth disp-mtx x)))

(defn long-cross
	"entire rank and entire file"
	[disp-mtx location]
	(concat
		(rank disp-mtx location)
		(file disp-mtx location)))

(defn predicate-lag
	"returns a predicate which will convert the first 'false' value to true
	the value AFTER will be false"
	[pred]
	(let [lagval (volatile! true)]
		(fn [& args]
				(let [r @lagval]
					(vreset! lagval (apply pred args))
					r))))

(defmethod movelocs-for-piece :piecetype/cannon
	[disp-mtx {:disposition/keys [location]}]
	(map filter-cannon-locs (long-cross disp-mtx location)))

(defmethod movelocs-for-piece :piecetype/chariot
	[disp-mtx {:disposition/keys [location]}]
	(keep #(seq (take-while (predicate-lag unoccupied-loc?) %))
		(long-cross disp-mtx location)))

(defn horse-move
	[disp-mtx {:location/keys [x y]} [leg1 & leg2s]]
	(let [{:location/keys [x y] :as loc1} (get-in disp-mtx (apply-locfn [x y] leg1))]
		(when (and loc1 (unoccupied-loc? loc1))
			(keep #(get-in disp-mtx (apply-locfn [x y] %)) leg2s))))

#_(defn apply-locfns
		[loc loc-fns]
		(map #(apply-locfn loc %)
			loc-fns))

#_(def diagonal-fns
		[[dec dec]
		 [inc inc]
		 [inc dec]
		 [dec inc]])

#_(defn short-cross2
		[disp-mtx {:location/keys [x y]}]
		(keep #(get-in disp-mtx %)
			(apply-locfns diagonal-fns [x y])))

(defmethod movelocs-for-piece :piecetype/horse
	[disp-mtx {:disposition/keys [location]}]
	(keep #(horse-move disp-mtx location %)
		[[[dec nil] [dec dec] [dec inc]]
		 [[inc nil] [inc dec] [inc inc]]
		 [[nil dec] [inc dec] [dec dec]]
		 [[nil inc] [inc inc] [dec inc]]]))

(defmethod movelocs-for-piece :piecetype/elephant
	[disp-mtx {:disposition/keys [piece location] :as disp}]
	(let [us (piece-owner piece)]
		(map list
			(filter #(= us (:location/player %))
				(double-diagonals disp-mtx location)))))

(defmethod movelocs-for-piece :piecetype/guard
	[disp-mtx {:disposition/keys [location]}]
	(->> (single-diagonals disp-mtx location)
		(filter :location/palace)
		(map list)))

(defmethod movelocs-for-piece :piecetype/general
	[disp-mtx {:disposition/keys [location]}]
	(->> (single-cross disp-mtx location)
		(filter :location/palace)
		(map list)))

(defn occupied-loc
	[disp-loc coord]
	(when (and disp-loc coord)
		(:disposition/piece (get-in disp-loc coord))))


(comment
	(def rd (xb/retrieve-disposition (dca/db xiangqi.datomic.peer/conn) :board/starting))
	(def cd (xb/collate-disposition rd))
	)