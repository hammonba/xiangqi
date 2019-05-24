(ns xiangqi.board-utils
  "isolates moving logic from specific board implementaitns"
  (:require [clojure.set]))

(defn invert-map-of-sets
  [map-of-sets]
  (into {}
    (mapcat (fn [[p ls]] (map #(vector % p) ls)))
    map-of-sets))

;; board constants
(def owner->location
  {:player/black #{:location/a10 :location/a9 :location/a8 :location/a7 :location/a6
                   :location/b10 :location/b9 :location/b8 :location/b7 :location/b6
                   :location/c10 :location/c9 :location/c8 :location/c7 :location/c6
                   :location/d10 :location/d9 :location/d8 :location/d7 :location/d6
                   :location/e10 :location/e9 :location/e8 :location/e7 :location/e6
                   :location/f10 :location/f9 :location/f8 :location/f7 :location/f6
                   :location/g10 :location/g9 :location/g8 :location/g7 :location/g6
                   :location/h10 :location/h9 :location/h8 :location/h7 :location/h6
                   :location/i10 :location/i9 :location/i8 :location/i7 :location/i6}
   :player/red #{:location/a5 :location/a4 :location/a3 :location/a2 :location/a1
                 :location/b5 :location/b4 :location/b3 :location/b2 :location/b1
                 :location/c5 :location/c4 :location/c3 :location/c2 :location/c1
                 :location/d5 :location/d4 :location/d3 :location/d2 :location/d1
                 :location/e5 :location/e4 :location/e3 :location/e2 :location/e1
                 :location/f5 :location/f4 :location/f3 :location/f2 :location/f1
                 :location/g5 :location/g4 :location/g3 :location/g2 :location/g1
                 :location/h5 :location/h4 :location/h3 :location/h2 :location/h1
                 :location/i5 :location/i4 :location/i3 :location/i2 :location/i1}})

(def location->owner (invert-map-of-sets owner->location))

(def location-ispalace
  #{:location/d10 :location/d9 :location/d8
    :location/e10 :location/e9 :location/e8
    :location/f10 :location/f9 :location/f8
    :location/d3 :location/d2 :location/d1
    :location/e3 :location/e2 :location/e1
    :location/f3 :location/f2 :location/f1})

(def owner->piece
  {:player/black #{:piecename/black-catapult
                   :piecename/black-chariot
                   :piecename/black-elephant
                   :piecename/black-general
                   :piecename/black-guard
                   :piecename/black-horse
                   :piecename/black-private}
   :player/red #{:piecename/red-cannon
                 :piecename/red-chariot
                 :piecename/red-guard
                 :piecename/red-horse
                 :piecename/red-marshal
                 :piecename/red-minister
                 :piecename/red-soldier}})

(def piece->owner (invert-map-of-sets owner->piece))

(def piecetype->piece
  {:piecetype/soldier #{:piecename/red-soldier :piecename/black-private}
   :piecetype/cannon #{:piecename/red-cannon :piecename/black-catapult}
   :piecetype/chariot #{:piecename/red-chariot :piecename/black-chariot}
   :piecetype/horse #{:piecename/red-horse :piecename/black-horse}
   :piecetype/elephant #{:piecename/red-minister :piecename/black-elephant}
   :piecetype/guard #{:piecename/red-guard :piecename/black-guard}
   :piecetype/general #{:piecename/red-marshal :piecename/black-general}})

(def piece->piecetype (invert-map-of-sets piecetype->piece))

(def opponent
  {:player/red :player/black
   :player/black :player/red})

;; data accessors

(defn disppiece
  [dl]
  (:disposition/piece dl))

(defn disppiecetype
  [dl]
  (piece->piecetype (:disposition/piece dl)))

(defn displocation
  [dl]
  (:disposition/location dl))

(defn unoccupied-loc?
  [loc]
  (nil? (disppiece loc)))

(def occupied-loc? (complement unoccupied-loc?))

(defn occupied-loc-bit
  "empty location is zero, occupied is 1"
  [l]
  (if (occupied-loc? l) 1 0))

(defn disploc-pieceowner
  [dl]
  (piece->owner (disppiece dl)))

(defn disploc-locationowner
  [dl]
  (location->owner (displocation dl)))

(defn is-disploc-pieceowner?
  [player dl]
  (= player (disploc-pieceowner dl)))

(defn is-disploc-piecetype?
  [piecetype disploc]
  (= piecetype (disppiecetype disploc)))

(defn find-general
  [disp-mtx player]
  (->>
    (flatten disp-mtx)
    (filter #(is-disploc-pieceowner? player %))
    (filter #(is-disploc-piecetype? :piecetype/general %))
    (first)))

(defn pieces-for-player
  [player disp-vec]
  (filter #(is-disploc-pieceowner? player %)
    disp-vec))

(def starting-board
  "the locations of the pieces at the start"
  {:location/a1 :piecename/red-chariot
   :location/b1 :piecename/red-horse
   :location/c1 :piecename/red-minister
   :location/d1 :piecename/red-guard
   :location/e1 :piecename/red-marshal
   :location/f1 :piecename/red-guard
   :location/g1 :piecename/red-minister
   :location/h1 :piecename/red-horse
   :location/i1 :piecename/red-chariot

   :location/b3 :piecename/red-cannon
   :location/h3 :piecename/red-cannon

   :location/a4 :piecename/red-soldier
   :location/c4 :piecename/red-soldier
   :location/e4 :piecename/red-soldier
   :location/g4 :piecename/red-soldier
   :location/i4 :piecename/red-soldier

   :location/a7 :piecename/black-private
   :location/c7 :piecename/black-private
   :location/e7 :piecename/black-private
   :location/g7 :piecename/black-private
   :location/i7 :piecename/black-private

   :location/b8 :piecename/black-catapult
   :location/h8 :piecename/black-catapult

   :location/a10 :piecename/black-chariot
   :location/b10 :piecename/black-horse
   :location/c10 :piecename/black-elephant
   :location/d10 :piecename/black-guard
   :location/e10 :piecename/black-general
   :location/f10 :piecename/black-guard
   :location/g10 :piecename/black-elephant
   :location/h10 :piecename/black-horse
   :location/i10 :piecename/black-chariot
   })

(defn nesting-map
  [f]
  (map (fn [outer] (map (fn [inner] (f inner)) outer))))