(ns xiangqi.board-layout
  (:require [medley.core :as medley]
            [xiangqi.board-utils :as board-utils]))

(def piece-owners (medley/map-keys name board-utils/piece->owner))

(def colour-classes
  {:player/red "red-piece"
   :player/black "black-piece"})

(def opened-move-classes
  {:player/red "red-opened-move"
   :player/black "black-opened-move"})

(defn piecehalo-class
  [piece]
  (cond (empty? (:piece/moves piece)) "counter"
        ;(:did-piece-open-move? piece) "has-moves"
        :else "counter has-moves"))

(defn place-singlepiece
  [openmove-fn {:keys [player]
                :piece/keys [piecename]
                :location/keys [x y] :as piece}]
  (openmove-fn [:g {:class (colour-classes player)}
                [:circle {:cx x :cy y :r 0.5 :class (piecehalo-class piece)}]
                [:use {:x x :y y :href (str "#" (name piecename))}]]
    piece))

(defn place-movedestination
  [decorate-fn opened-move {:move/keys [x y] :as move}]
  (decorate-fn
    [:circle {:cx (or x (:location/x move))
              :cy (or y (:location/y move))
              :r 0.5 :class "opened-move"}]
    opened-move
    move))

(defn group-pieces-by-player
  [disposition]
  (group-by (comp piece-owners :piece/piecename) disposition))

(defn layout-pieces
  [{:keys [openmove-fn completemove-fn did-piece-open-move?-fn] }
   disposition
   opened-move
   completable-moves]
  (let [{:player/keys [red black]} (group-pieces-by-player disposition)]
    [:g {:id "pieces"}
     (into [:g {:id "red-pieces" :fill "red" :stroke "red"}]
       (map #(place-singlepiece
               openmove-fn
               (assoc % :player :player/red
                        :did-piece-open-move? (did-piece-open-move?-fn %))))
       red)
     (into [:g {:id "black-pieces" :fill "black" :stroke "black"}]
       (map #(place-singlepiece
               openmove-fn
               (assoc % :player :player/black
                        :did-piece-open-move? (did-piece-open-move?-fn %))))
       black)
     (into [:g {:id "opened-move"}]
       (map #(place-movedestination completemove-fn opened-move %1))
       completable-moves)]))

(def boundary-class
  {:player/red "red-boundary"
   :player/black "black-boundary"})

(defn board-hiccup
  [{:board/keys [player]} laidout-pieces]
  [:g {:viewBox "0 0 9 10" :preserveAspectRatio "xMidYMid meet"}
   [:g {:transform "translate(0.5,0.5)"}
    [:g {:id "boundary" :class (boundary-class player)}
     [:rect {:x -0.2 :y -0.2 :width 8.4 :height 0.2}]
     [:rect {:x -0.2 :y -0.2 :width 0.2 :height 4.2}]
     [:rect {:x -0.2 :y 5 :width 0.2 :height 4.2}]
     [:rect {:x -0.2 :y 9 :width 8.4 :height 0.2}]
     [:rect {:x 8 :y -0.2 :width 0.2 :height 4.2}]
     [:rect {:x 8 :y 5 :width 0.2 :height 4.2}]]
    [:g {:class "board"}
     [:use {:href "/board.svg#board"}]]
    laidout-pieces
    ]])


#_(defn board-hiccup-fullhtml
  [{:board/keys [player]} laidout-pieces]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "/css/style.css"}]]
   [:svg {:viewBox "0 0 900 1000" :preserveAspectRatio "xMidYMid meet"}
    [:g {:transform "scale(100,100) translate(0.5,0.5)"}
     [:g {:id "boundary" :class (boundary-class player)}
      [:rect {:x -0.5 :y -0.5 :width 9 :height 0.5}]
      [:rect {:x -0.5 :y -0.5 :width 0.5 :height 4.5}]
      [:rect {:x -0.5 :y 5 :width 0.5 :height 4.5}]
      [:rect {:x -0.5 :y 9 :width 9 :height 0.5 }]
      [:rect {:x 8 :y -0.5 :width 0.5 :height 4.5 }]
      [:rect {:x 8 :y 5 :width 0.5 :height 4.5 }]]
     [:g {:class "board"}
      [:use {:href "/board.svg#board"}]]
     laidout-pieces
     ]]])


