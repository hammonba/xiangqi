(ns xiangqi.board.board-ident
  "serialise board layout into a single number. also deserialise"
  (:require [clojure.set]
            [medley.core :as medley]
            [xiangqi.board.bit-utils :as bitutils]
            [xiangqi.board-utils :as boardutils]))

;; encoding constants
(def player->hashnumber
  {:player/red 0N
   :player/black 1N})

(def hashnumber->player
  (clojure.set/map-invert player->hashnumber))

(def piece->hashnumber
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

(def hashnumber->piece
  (clojure.set/map-invert piece->hashnumber))

(def ordered-locs
  [:location/a1 :location/a2 :location/a3 :location/a4 :location/a5 :location/a6 :location/a7 :location/a8 :location/a9 :location/a10
   :location/b1 :location/b2 :location/b3 :location/b4 :location/b5 :location/b6 :location/b7 :location/b8 :location/b9 :location/b10
   :location/c1 :location/c2 :location/c3 :location/c4 :location/c5 :location/c6 :location/c7 :location/c8 :location/c9 :location/c10
   :location/d1 :location/d2 :location/d3 :location/d4 :location/d5 :location/d6 :location/d7 :location/d8 :location/d9 :location/d10
   :location/e1 :location/e2 :location/e3 :location/e4 :location/e5 :location/e6 :location/e7 :location/e8 :location/e9 :location/e10
   :location/f1 :location/f2 :location/f3 :location/f4 :location/f5 :location/f6 :location/f7 :location/f8 :location/f9 :location/f10
   :location/g1 :location/g2 :location/g3 :location/g4 :location/g5 :location/g6 :location/g7 :location/g8 :location/g9 :location/g10
   :location/h1 :location/h2 :location/h3 :location/h4 :location/h5 :location/h6 :location/h7 :location/h8 :location/h9 :location/h10
   :location/i1 :location/i2 :location/i3 :location/i4 :location/i5 :location/i6 :location/i7 :location/i8 :location/i9 :location/i10])

(def disposition-piece-hashnum-xform
  (comp
    (filter boardutils/occupied-loc?)
    (map boardutils/disppiece)
    (map piece->hashnumber)))

(defn layout-piece-seq
  "we explicitly know that pieces can be described as 4 bits
   and we can squeeze 16 of them into a single long ..."
  [disp-mtx]
  (sequence
    (comp disposition-piece-hashnum-xform
      (partition-all 16)
      (map #(bitutils/encode-shortsseq-as-long 4 %)))
    (flatten disp-mtx)))

(defn compute-boardident-filemask
  "one file is 10 locations from black across river to red"
  [f]
  (bitutils/encode-shortsseq-as-long 1 (map boardutils/occupied-loc-bit f)))

(defn compute-boardident-mask-seq
  "describe layout of pieces in 90 bits; 0 for empty, 1 for occupied
   disp-mtx is expected to 9 * 10 element vectors, hence the magic number 6
   will take 60 locations and encode them into a 60 bit long"
  [disp-mtx]
  (sequence
    (comp
      (partition-all 6)
      (map flatten)
      (map compute-boardident-filemask))
    disp-mtx))

(defn encode-boardident-218
  "encodes state of play into 218 bit bigint"
  [{:board/keys [player disp-mtx]}]
  (reduce bitutils/bigint-bitshift-add
    (player->hashnumber player)
    (concat
      (layout-piece-seq disp-mtx)
      (compute-boardident-mask-seq disp-mtx))))

(defn decode-boardident-mask
  "convert 90bit number into array of 90 booleans"
  [n]
  (let [barr (boolean-array 90)]
    [barr (reduce (fn [n c]
                    (aset barr c (pos? (mod n 2)))
                    (quot n 2))
            n
            (range 89 -1 -1))]))

(defn decode-boardidend-file
  [mask n]
  (let [arr (object-array (count mask))]
    [arr (reduce
           (fn [n c]
             (if (aget mask c)
               (do (aset arr c (hashnumber->piece (mod n 16)))
                   (quot n 16))
               n))
           n
           (range (dec (count mask)) -1 -1))]))

(defn build-mtx-from-vec
  "split into matrix where each row is length 10"
  [v]
  (mapv vec (partition 10 v)))

(defn build-boardvec
  [piece-seq]
  (mapv
    (fn [p l idx]
      (medley/assoc-some
        {:disposition/location l}
        :disposition/piece p
        :location/index idx
        :location/x (quot idx 10)
        :location/y (mod idx 10)))
    piece-seq
    ordered-locs
    (range)))

(defn add-dispmtx
  [{:board/keys [disp-vec] :as board}]
  (assoc board :board/disp-mtx
               (build-mtx-from-vec disp-vec)))

(defn decode-boardident-218
  "recreates state of play from bigint"
  [n]
  (let [[m n2] (decode-boardident-mask n)
        [f p] (decode-boardidend-file m n2)]
    {:board/ident n
     :board/player (hashnumber->player p)
     :board/disp-vec (build-boardvec f)}))

(def initial-board-218
  "the initial board state, in 218 bits"
  133533018309899709564525504784527298902009500465768741687617263177N)

(def initial-board-edn
  "the initial board state. In plain old edn"
  (decode-boardident-218 initial-board-218))

(comment (= (board-ident/decode-boardident-218 (:board/ident board-ident/initial-board-edn))
           board-ident/initial-board-edn))

(comment
  (mapcat #(map (fn [{id :disposition/location :location/keys [x y]
                      :as loc}]
                  (-> loc
                    (medley/assoc-some
                      :db/id id
                      :location/player (xiangqi.board-utils/location->owner id)
                      :location/palace (when-let [b (xiangqi.board-utils/location-ispalace id)] b)
                      :location/north (get-in bi/initial-board-edn [:board/disposition x (dec y) :disposition/location])
                      :location/south (get-in bi/initial-board-edn [:board/disposition x (inc y) :disposition/location])
                      :location/east (get-in bi/initial-board-edn [:board/disposition (dec x) y :disposition/location])
                      :location/west (get-in bi/initial-board-edn [:board/disposition (inc x) y :disposition/location])
                      )
                    (dissoc :disposition/location :location/index :disposition/piece))) %)
    (:board/disposition bi/initial-board-edn))
  )
