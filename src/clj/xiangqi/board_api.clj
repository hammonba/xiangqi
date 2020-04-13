(ns xiangqi.board-api
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as test.gen]
            [io.pedestal.http.route :as http.route]
            [medley.core :as medley]
            [xiangqi.board-layout :as board-layout]
            [xiangqi.board-utils :as board-utils]
            [xiangqi.board.bit-utils :as bit-utils]
            [xiangqi.board.board-ident :as board-ident]
            [xiangqi.board.movecalc :as movecalc]
            [xiangqi.pedestal.board]
            [xiangqi.utils :as utils])
  (:import [clojure.lang BigInt]))

(defn decode-board
  [board-id]
  (-> board-id
    bit-utils/decodestring-into-bigint
    board-ident/decode-boardident-218
    (assoc :board/ident-string board-id)
    (->> (spec/conform ::board))))

(defn maybe-addin-moves
  "if this location contains a piece for specified player,
  then include moves"
  [{:board/keys [player] :as board} disploc]
  (if (= player (board-utils/disploc-pieceowner disploc))
    (assoc disploc
      :piece/all-movechains
      (movecalc/compute-and-generate-moves-for-piece board disploc))
    disploc))

(defn addin-moves
  [board]
  (let [mb (board-ident/add-dispmtx board)]
    (update board
      :board/disp-vec
      (board-utils/nesting-mapv
        (fn [dl] (maybe-addin-moves mb dl))))))

(defn moves
  [board-id]
  (-> board-id
    decode-board
    addin-moves
    (->> (spec/conform ::board))))

(def initial
  (bit-utils/encodebigint-as-string board-ident/initial-board-218))

(defn location-vecindex
  [loc-str]
  (when loc-str
    (-> (keyword "location" loc-str)
      board-ident/loc2index)))

(defn mark-openedmove
  [{:board/keys [disp-vec] :as board} open]
  (if-let [idx (location-vecindex open)]
    (let [disp-elt (assoc (nth disp-vec idx)
                     :move/opened true)]
      (-> board
        (assoc-in [:board/disp-vec idx] disp-elt)
        (assoc :board/opened-move disp-elt)))
    board))

(defn remove-unoccupied
  "create new key :board/disposition that ONLY contains
   the occupied pieces.
   Removes :board/disp-vec to make this absolutely obvious"
  [{:board/keys [disp-vec] :as board}]
  (-> board
    (dissoc :board/disp-vec)
    (assoc :board/disposition
           (filter :disposition/piece disp-vec))))

(defn open-move
  "immure the svg inside an html anchor that will open the move"
  [ident-string svg {:piece/keys [location]}]
  [:a {:href (http.route/url-for :board.v1/html
               :path-params {:board-ident ident-string}
               :query-params {:open (utils/nname location)})}
   svg])

(defn complete-move
  "immure the svg inside an html anchor that will jump to the next board"
  [svg opened-move {:keys [board-after]}]
  [:a {:href (http.route/url-for :board.v1/html
               :path-params {:board-ident board-after})}
   svg])

(defn group-pieces-by-player
  [disposition]
  (group-by (comp board-utils/piece->owner
              :disposition/piece)
    disposition))

(defn create-openmove-url
  [location]
  (http.route/url-for :board.v1/html
    :query-params {:open (utils/nname location)}))

(defn create-completemove-url
  [{:move/keys [board-after]}]
  (http.route/url-for :board.v1/html
    :path-params {:board-id board-after}))

(defn wrapwith-openmove-anchor
  "wrap svg for a piece within an html anchor"
  [svg openmove-url]
  (if (some? openmove-url)
    [:a {:href openmove-url} svg]
    svg))

(defn maybe-create-openmove-url
  [disp-elt]
  (when-not (empty? (:piece/all-movechains disp-elt))
    (create-openmove-url (:disposition/location disp-elt))))

(defn pieces-for-player
  [{:board/keys [disposition]}]
  (binding [board-layout/*href-root* "/board.svg"]
    (medley/map-vals
      (fn [disp-elts]
          (mapv #(wrapwith-openmove-anchor
                   (board-layout/piece-svg  %)
                   (maybe-create-openmove-url %))
            disp-elts))
      (group-pieces-by-player disposition))))

(defn board-hiccup
  [{:board/keys [player opened-move]} {:player/keys [red black]}]
  [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 12"}
   [:g {:viewBox "0 0 9 10" :preserveAspectRatio "xMidYMid meet"}
    [:g {:transform "translate(0.5,0.5)"}
     [:g {:id "boundary" :class (board-layout/boundary-class player)}
      [:rect {:x -0.2 :y -0.2 :width 8.4 :height 0.2}]
      [:rect {:x -0.2 :y -0.2 :width 0.2 :height 4.2}]
      [:rect {:x -0.2 :y 5 :width 0.2 :height 4.2}]
      [:rect {:x -0.2 :y 9 :width 8.4 :height 0.2}]
      [:rect {:x 8 :y -0.2 :width 0.2 :height 4.2}]
      [:rect {:x 8 :y 5 :width 0.2 :height 4.2}]]
     [:g {:class "board"}
      [:use {:href "/board.svg#board"}]]
     [:g {:id "pieces"}
      (into [:g {:id "red-pieces" :fill "red" :stroke "red"}] red)
      (into [:g {:id "black-pieces" :fill "black" :stroke "black"}] black)]
     (when opened-move
       (into [:g {:id "opened-move"}]
         (comp
           (remove :move/illegal?)
           (map #(wrapwith-openmove-anchor
                   (board-layout/completemove-svg %)
                   (create-completemove-url %)) ))
         (flatten (:piece/all-movechains opened-move)))
       )]]])

(defn board-hiccup-with-pieces
  [b]
  (board-hiccup b (pieces-for-player b)))

(defn stylesheet-link
  [href]
  [:link {:href href :rel "stylesheet" :type "text/css"}])

(defn add-enclosing-html
  [& body]
  [:html
   [:head (stylesheet-link "/css/style.css")]
   (into [:body] body)])

(defn html
  [board-id open]
  (-> (moves board-id)
    (mark-openedmove open)
    remove-unoccupied
    board-hiccup-with-pieces
    add-enclosing-html
    hiccup2.core/html
    str))

(defn describe
  "format board data for React GUI"
  [board-id]
  (-> board-id
    moves
    remove-unoccupied
    (update :board/disposition group-pieces-by-player)))

(spec/def :board/ident-218
  (spec/spec #(instance? BigInt %)
    :gen (constantly '(test.gen/fmap bigint (spec/gen pos-int?)))))


(spec/def :board/ident-string string?)
(spec/def :board/player #{:player/red :player/black})

(spec/def :board/winner :board/player)
(spec/def :board/loser :board/player)


(spec/def :board/disp-vec
  (spec/coll-of :board/disp-elt :count 90))

(spec/def ::locations
  #{:location/a1 :location/a2 :location/a3 :location/a4 :location/a5 :location/a6 :location/a7 :location/a8 :location/a9 :location/a10
    :location/b1 :location/b2 :location/b3 :location/b4 :location/b5 :location/b6 :location/b7 :location/b8 :location/b9 :location/b10
    :location/c1 :location/c2 :location/c3 :location/c4 :location/c5 :location/c6 :location/c7 :location/c8 :location/c9 :location/c10
    :location/d1 :location/d2 :location/d3 :location/d4 :location/d5 :location/d6 :location/d7 :location/d8 :location/d9 :location/d10
    :location/e1 :location/e2 :location/e3 :location/e4 :location/e5 :location/e6 :location/e7 :location/e8 :location/e9 :location/e10
    :location/f1 :location/f2 :location/f3 :location/f4 :location/f5 :location/f6 :location/f7 :location/f8 :location/f9 :location/f10
    :location/g1 :location/g2 :location/g3 :location/g4 :location/g5 :location/g6 :location/g7 :location/g8 :location/g9 :location/g10
    :location/h1 :location/h2 :location/h3 :location/h4 :location/h5 :location/h6 :location/h7 :location/h8 :location/h9 :location/h10
    :location/i1 :location/i2 :location/i3 :location/i4 :location/i5 :location/i6 :location/i7 :location/i8 :location/i9 :location/i10})

(spec/def :disposition/location ::locations)

(spec/def ::piecenames
  #{:piecename/red-soldier :piecename/red-cannon :piecename/red-chariot
    :piecename/red-horse :piecename/red-minister :piecename/red-guard
    :piecename/red-marshal :piecename/black-private :piecename/black-catapult
    :piecename/black-chariot :piecename/black-horse :piecename/black-elephant
    :piecename/black-guard :piecename/black-general})

(spec/def :disposition/piece ::piecenames)

(spec/def :location/index (spec/int-in 0 90))
(spec/def :location/x (spec/int-in 0 9))
(spec/def :location/y (spec/int-in 0 10))
(spec/def :location/move-opened? boolean?)

(spec/def :board/disp-elt
  (spec/keys
    :req [:disposition/location
          :location/index
          :location/x
          :location/y]
    :opt [:disposition/piece
          :location/move-opened?
          :location/complete-move
          :piece/all-movechains]))


(spec/def :move/piece-moved ::piecenames)
(spec/def :move/piece-taken ::piecenames)
(spec/def :move/start-location :board/disp-elt)
(spec/def :move/end-location :board/disp-elt)
(spec/def :move/board-before :board/ident-string)
(spec/def :move/board-after :board/ident-string)
(spec/def :move/ordering nat-int?)
(spec/def :move/illegal? #{:move/checked-by :move/flying-general?})
(spec/def :move/flying-general? boolean?)
(spec/def :move/checked-by :disposition/location)

(spec/def :piece/move
  (spec/keys
    :req [:move/piece-moved
          :move/start-location
          :move/end-location
          :move/board-before]
    :opt [:move/board-after
          :move/illegal?
          :move/flying-general?
          :move/checked-by
          :move/piece-taken
          :move/ordering]
    ))
(spec/def :location/complete-move :piece/move)

(spec/def :piece/movechain
  (spec/coll-of :piece/move :max-count 10 :distinct true))

;; for a cannon or a chariot that can move to multiple locations
;; in a number of differenc directions; each direction gets its own
;; movechain
(spec/def :piece/all-movechains
  (spec/coll-of :piece/movechain :max-count 4))

(spec/def ::board
  (spec/keys
    :req [:board/ident-218
          :board/ident-string
          :board/player
          :board/disp-vec]
    :opt [:board/winner
          :board/loser]))
