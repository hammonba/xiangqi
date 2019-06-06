(ns client.board-controller
  (:require [citrus.core :as citrus]))

(def initial-state
  {:msg-type :board,
   :board-ident "pgHgrPgmHQSuzVg9ogvpnhxv5CppZ3D5RbxSK",
   :board/player :player/red,
   :disposition [{:piece/piecename "red-chariot", :location/x 0, :location/y 0, :piece/location :location/a1,
                  :piece/moves [#:move{:x 0, :y 1, :next-board "HdwuF8JVv1dBsNQmNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 0, :y 2, :next-board "z1bW2f7SAxsAu4imNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "red-soldier", :location/x 0, :location/y 3, :piece/location :location/a4,
                  :piece/moves [#:move{:x 0, :y 4, :next-board "XzCHyryM75wC9mNnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "black-private", :location/x 0, :location/y 6, :piece/location :location/a7,
                  :piece/moves '()}
                 {:piece/piecename "black-chariot", :location/x 0, :location/y 9, :piece/location :location/a10,
                  :piece/moves '()}
                 {:piece/piecename "red-horse", :location/x 1, :location/y 0, :piece/location :location/b1,
                  :piece/moves [#:move{:x 2, :y 2, :next-board "kPGAbJVpPZSFLkwLZs1U9ge1kLS2LU7CYTBg94"}
                                #:move{:x 0, :y 2, :next-board "HwXTmtbNfMdkVvrTJ2DY4MhZovv6hNur3Yjtc4"}]}
                 {:piece/piecename "red-cannon", :location/x 1, :location/y 2, :piece/location :location/b3,
                  :piece/moves [#:move{:x 0, :y 2, :next-board "FXag2SZaCtq9fuzkcM4TWCeVSBC2RiCVrvUia4"}
                                #:move{:x 2, :y 2, :next-board "48hN7tqYRkwWXANjxEWZy6Qok7jdnif4ZEDg94"}
                                #:move{:x 3, :y 2, :next-board "kBqTR4WBxJAfq4JZYp4k8fouNwBAcaJ1ZEDg94"}
                                #:move{:x 4, :y 2, :next-board "vfjeguGGDvZZkDU5QnN7wrXaZMg7PaJ1ZEDg94"}
                                #:move{:x 5, :y 2, :next-board "VqZLuLs9txWdP6dHDsFxys4HBYz5PaJ1ZEDg94"}
                                #:move{:x 6, :y 2, :next-board "P4rXVuGce8FmK3UtYfTyrmf32Zz5PaJ1ZEDg94"}
                                #:move{:x 1, :y 1, :next-board "MQzXNajdKPyUbjWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 1, :y 3, :next-board "633ryMhtDZLuugWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 1, :y 4, :next-board "DJWkRdsbi2Pq3gWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 1, :y 5, :next-board "V5pNuW2383QoPfWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 1, :y 6, :next-board "4gCwjNPXwxQSGfWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 1, :y 9, :next-board "paWjQGtfNFasRPeeaTWzZctdaxyxwnVeheJoc"}]}
                 {:piece/piecename "black-catapult", :location/x 1, :location/y 7, :piece/location :location/b8,
                  :piece/moves '()}
                 {:piece/piecename "black-horse", :location/x 1, :location/y 9, :piece/location :location/b10,
                  :piece/moves '()}
                 {:piece/piecename "red-minister", :location/x 2, :location/y 0, :piece/location :location/c1,
                  :piece/moves [#:move{:x 4, :y 2, :next-board "Xv9AZGSsyRYLAkf1WHTynVJkZRUcvxyXXCDg94"}
                                #:move{:x 0, :y 2, :next-board "ayY5doLb7sdthGpKxp3Kjd2GjxCpXWzuSm3yd4"}]}
                 {:piece/piecename "red-soldier", :location/x 2, :location/y 3, :piece/location :location/c4,
                  :piece/moves [#:move{:x 2, :y 4, :next-board "aET4JBQDiieBohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "black-private", :location/x 2, :location/y 6, :piece/location :location/c7,
                  :piece/moves '()}
                 {:piece/piecename "black-elephant", :location/x 2, :location/y 9, :piece/location :location/c10,
                  :piece/moves '()}
                 {:piece/piecename "red-guard", :location/x 3, :location/y 0, :piece/location :location/d1,
                  :piece/moves [#:move{:x 4, :y 1, :next-board "gxPr8wEjp6Uh1XEU4scdY6pRtpj4VF19YCDg94"}]}
                 {:piece/piecename "black-guard", :location/x 3, :location/y 9, :piece/location :location/d10,
                  :piece/moves '()}
                 {:piece/piecename "red-marshal", :location/x 4, :location/y 0, :piece/location :location/e1,
                  :piece/moves [#:move{:x 4, :y 1, :next-board "6uiMu3uRiWGCohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "red-soldier", :location/x 4, :location/y 3, :piece/location :location/e4,
                  :piece/moves [#:move{:x 4, :y 4, :next-board "8Kt8qF2MkWGCohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "black-private", :location/x 4, :location/y 6, :piece/location :location/e7,
                  :piece/moves '()}
                 {:piece/piecename "black-general", :location/x 4, :location/y 9, :piece/location :location/e10,
                  :piece/moves '()}
                 {:piece/piecename "red-guard", :location/x 5, :location/y 0, :piece/location :location/f1,
                  :piece/moves [#:move{:x 4, :y 1, :next-board "H3zD9LeoG3utuh7XF6d3DZbKgu8VQF19YCDg94"}]}
                 {:piece/piecename "black-guard", :location/x 5, :location/y 9, :piece/location :location/f10,
                  :piece/moves '()}
                 {:piece/piecename "red-minister", :location/x 6, :location/y 0, :piece/location :location/g1,
                  :piece/moves [#:move{:x 8, :y 2, :next-board "ppFy5s5ga24WawDou9QdMSrpZvzUQF19YCDg94"}
                                #:move{:x 4, :y 2, :next-board "XDERzZ7gS1iYYudf4FZsnMTGk22VQF19YCDg94"}]}
                 {:piece/piecename "red-soldier", :location/x 6, :location/y 3, :piece/location :location/g4,
                  :piece/moves [#:move{:x 6, :y 4, :next-board "i5fDVUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "black-private", :location/x 6, :location/y 6, :piece/location :location/g7,
                  :piece/moves '()}
                 {:piece/piecename "black-elephant", :location/x 6, :location/y 9, :piece/location :location/g10,
                  :piece/moves '()}
                 {:piece/piecename "red-horse", :location/x 7, :location/y 0, :piece/location :location/h1,
                  :piece/moves [#:move{:x 8, :y 2, :next-board "BgA49W4CxkBVSKJKysxiVFcuZvzUQF19YCDg94"}
                                #:move{:x 6, :y 2, :next-board "ed4PAXa777xxTgBxeh51YyquZvzUQF19YCDg94"}]}
                 {:piece/piecename "red-cannon", :location/x 7, :location/y 2, :piece/location :location/h3,
                  :piece/moves [#:move{:x 8, :y 2, :next-board "H1bT3xkCN2TyLdPWpmWs3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 1, :next-board "RATBYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 3, :next-board "4aTAYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 4, :next-board "6qHAYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 5, :next-board "BxCAYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 6, :next-board "n7AAYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 7, :y 9, :next-board "6qzEQWjdQUt25HqXqMiUSY8Yu63ru31LbeJoc"}
                                #:move{:x 6, :y 2, :next-board "caVUQzm7t1dtRfohkJhxbRguZvzUQF19YCDg94"}
                                #:move{:x 5, :y 2, :next-board "XHCarJsJmtYz3Lk7P7AtrxG2YvzUQF19YCDg94"}
                                #:move{:x 4, :y 2, :next-board "THDaHzqqoMhogqcTCVwGSMKZxCFUQF19YCDg94"}
                                #:move{:x 3, :y 2, :next-board "i9UX7c2UJ3dp9TurD7FDMkhX6nXFNF19YCDg94"}
                                #:move{:x 2, :y 2, :next-board "xMhKYyS9xRmmn3GXqPt8vsy1s2PMyPa9YCDg94"}]}
                 {:piece/piecename "black-catapult", :location/x 7, :location/y 7, :piece/location :location/h8,
                  :piece/moves '()}
                 {:piece/piecename "black-horse", :location/x 7, :location/y 9, :piece/location :location/h10,
                  :piece/moves '()}
                 {:piece/piecename "red-chariot", :location/x 8, :location/y 0, :piece/location :location/i1,
                  :piece/moves [#:move{:x 8, :y 1, :next-board "zydBYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}
                                #:move{:x 8, :y 2, :next-board "nwdBYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "red-soldier", :location/x 8, :location/y 3, :piece/location :location/i4,
                  :piece/moves [#:move{:x 8, :y 4, :next-board "rCdBYUX3mWGCohWnNnUj3GcuZvzUQF19YCDg94"}]}
                 {:piece/piecename "black-private", :location/x 8, :location/y 6, :piece/location :location/i7,
                  :piece/moves '()}
                 {:piece/piecename "black-chariot", :location/x 8, :location/y 9, :piece/location :location/i10,
                  :piece/moves '()}]})

(defmulti control-board (fn [event] (:action event event)))

(defmethod control-board :init
  [_]
  {:state initial-state})

(defmethod control-board :get-board
  [evt [args & _] state cofx]
  {:websocket-post (assoc args :action :get-board)})

(defmethod control-board :received-board
  [evt [msg & _] state cofx]
  {:state (merge state msg)})