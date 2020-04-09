(ns xiangqi.board.movecalc
  "the only public function is compute-and-generate-next-moves"
  (:require [medley.core :as medley]
            [xiangqi.board-utils :as board-utils]
            [xiangqi.board.board-ident :as board-ident]))

(defn coords-from-location
  [{:location/keys [x y]}]
  [x y])

(defn apply-locfn
  [[x y] [xfn yfn]]
  [(if xfn (xfn x) x)
   (if yfn (yfn y) y)])

(defn reordered-split-at
  "splits the coll at n
  excludes n
  arrange coll so is increasing distance from n"
  [n coll]
  [(reverse (take n coll)) (drop (inc n) coll)])

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

(defn single-cross
  "one space in every direction"
  [disp-mtx {:location/keys [x y]}]
  (keep #(get-in disp-mtx %)
    [[(dec x) y]
     [(inc x) y]
     [x (dec y)]
     [x (inc y)]]))

(defn single-diagonals
  "one space in every diagonal"
  [disp-mtx {:location/keys [x y]}]
  (keep #(get-in disp-mtx %)
    [[(dec x) (dec y)]
     [(inc x) (inc y)]
     [(inc x) (dec y)]
     [(dec x) (inc y)]]))

(defn double-diagonals
  "two spaces in every direction"
  [disp-mtx {:location/keys [x y]}]
  (keep #(get-in disp-mtx %)
    [[(- x 2) (- y 2)]
     [(+ x 2) (+ y 2)]
     [(- x 2) (+ y 2)]
     [(+ x 2) (- y 2)]]))

(defn predicate-lag
  "returns a predicate which will convert the first 'false' value to true
  the value AFTER will be false"
  [pred]
  (let [lagval (volatile! true)]
    (fn [& args]
        (let [r @lagval]
          (vreset! lagval (apply pred args))
          r))))

(defmulti movelocs-for-piece
  (fn [disp-mtx disp-loc] (board-utils/disppiecetype disp-loc)))

(defn are-generals-facing?
  "test whether generals are in the same rank with no intermediate pieces"
  [disp-mtx]
  (let [red-general (board-utils/find-general disp-mtx :player/red)
        black-general (board-utils/find-general disp-mtx :player/black)]
    (when (= (:location/x red-general) (:location/x black-general))
      (->> (file disp-mtx black-general)
        (map #(remove board-utils/unoccupied-loc? %))
        (some #(= :piecename/red-marshal (:disposition/piece (first %))))))))

(defn apply-move-to-disposition
  [disp-mtx {:move/keys [piece-moved start-location end-location] :as mv}]
  (-> disp-mtx
    (update-in (coords-from-location start-location) dissoc :disposition/piece)
    (update-in (coords-from-location end-location) assoc :disposition/piece piece-moved)))

(defn flatset-of-move-location-idents
  [disp-mtx disp-loc]
  (into #{}
    (comp cat
      (map :disposition/location))
    (movelocs-for-piece disp-mtx disp-loc)))

(defn is-move-check?
  "would this move put us into check?"
  [disp-mtx our-player]
  (let [our-general-loc (:disposition/location (board-utils/find-general disp-mtx our-player))]
    (reduce
      (fn [_ disp-loc]
          (when (contains?
                  (flatset-of-move-location-idents disp-mtx disp-loc)
                  our-general-loc)
            (reduced disp-loc)))
      nil
      (board-utils/pieces-for-player (board-utils/opponent our-player) (flatten disp-mtx)))))

(defn apply-boardlevel-movetest
  "would this move put us into check?
   does this move result in two generals directly facing each other?
   both should be avoided"
  [disp-mtx {:move/keys [piece-moved] :as move}]
  (let [mtx-next (apply-move-to-disposition disp-mtx move)
        checked-by (:disposition/location (is-move-check? mtx-next (board-utils/piece->owner piece-moved)))
        flying-general? (are-generals-facing? mtx-next)
        moving-player (board-utils/piece->owner piece-moved)
        illegal? (cond
                   (some? checked-by) :move/checked-by
                   (some? flying-general?) :move/flying-general?
                   :else nil)]
    (medley/assoc-some move
      :move/checked-by checked-by
      :move/flying-general? (when flying-general? true)
      :move/illegal? illegal?
      :move/end-board (when-not illegal?
                        (board-ident/encode-boardident-218
                          {:board/player (board-utils/opponent moving-player)
                           :board/disp-mtx mtx-next})))))

(defn generate-moves-for-piece
  "compute individual moves for piece
  take out those that land on one of our piece
  take out those that move us into check
  take out those that enable the antagonists to face each other
  "
  [disploc dest-locs]
  (let [us (board-utils/disploc-pieceowner disploc)]
    (sequence
      (comp
        (remove (fn [dest-loc]
                    (= us (board-utils/disploc-pieceowner dest-loc))))
        (map (fn [dest-loc]
                 (medley/assoc-some
                   {:move/piece-moved (board-utils/disppiece disploc)
                    :move/start-location disploc
                    :move/end-location dest-loc}
                   :move/piece-taken (board-utils/disppiece dest-loc)))))
      dest-locs)))

(defn compute-and-generate-moves-for-piece
  [{:board/keys [ident disp-mtx]} dl]
  (sequence
    (comp
      (keep #(seq (generate-moves-for-piece dl %)))
      (board-utils/nesting-map #(apply-boardlevel-movetest disp-mtx %))
      (board-utils/nesting-map #(assoc % :move/start-board ident)))
    (movelocs-for-piece disp-mtx dl)))

(def soldieradvance-fn
  {:player/red inc
   :player/black dec})

(defmethod movelocs-for-piece :piecetype/soldier
  [disp-mtx {:location/keys [x y] :as disploc}]
  (let [piece-owner (board-utils/disploc-pieceowner disploc)
        location-owner (board-utils/disploc-locationowner disploc)

        coords (cond-> [[nil (soldieradvance-fn piece-owner)]]
                 (not= piece-owner location-owner) (conj [dec nil] [inc nil]))]
    (->> coords
      (mapv #(apply-locfn [x y] %))
      (keep #(get-in disp-mtx %))
      (map list))))

(defn filter-cannon-locs
  [loc-coll]
  (let [[f r] (split-with board-utils/unoccupied-loc? loc-coll)
        t (second (filter board-utils/occupied-loc? r))]
    (if (nil? t)
      f
      (concat f (list t)))))

(defmethod movelocs-for-piece :piecetype/cannon
  [disp-mtx disploc]
  (map filter-cannon-locs (long-cross disp-mtx disploc)))

(defmethod movelocs-for-piece :piecetype/chariot
  [disp-mtx disploc]
  (keep #(seq (take-while (predicate-lag board-utils/unoccupied-loc?) %))
    (long-cross disp-mtx disploc)))

(defn horse-move
  [disp-mtx {:location/keys [x y]} [leg1 & leg2s]]
  (let [{:location/keys [x y] :as loc1} (get-in disp-mtx (apply-locfn [x y] leg1))]
    (when (and loc1 (board-utils/unoccupied-loc? loc1))
      (keep #(get-in disp-mtx (apply-locfn [x y] %)) leg2s))))

(defmethod movelocs-for-piece :piecetype/horse
  [disp-mtx disploc]
  (keep #(horse-move disp-mtx disploc %)
    [[[dec nil] [dec dec] [dec inc]]
     [[inc nil] [inc dec] [inc inc]]
     [[nil dec] [inc dec] [dec dec]]
     [[nil inc] [inc inc] [dec inc]]]))

(defmethod movelocs-for-piece :piecetype/elephant
  [disp-mtx disploc]
  (let [us (board-utils/disploc-pieceowner disploc)]
    (map list
      (filter #(= us (board-utils/disploc-locationowner %))
        (double-diagonals disp-mtx disploc)))))

(defmethod movelocs-for-piece :piecetype/guard
  [disp-mtx disploc]
  (->> (single-diagonals disp-mtx disploc)
    (filter (comp board-utils/location-ispalace :disposition/location))
    (map list)))

(defmethod movelocs-for-piece :piecetype/general
  [disp-mtx disploc]
  (->> (single-cross disp-mtx disploc)
    (filter (comp board-utils/location-ispalace :disposition/location))
    (map list)))

(defn compute-and-generate-next-moves
  "main entry point to compute next moves.
   ALL OTHER FUNCTIONS SHOULD BE CONSIDERED PRIVATE"
  [{:board/keys [player disp-vec] :as board}]
  (let [board (board-ident/add-dispmtx board)]
    (into []
      (map #(compute-and-generate-moves-for-piece board %))
      (board-utils/pieces-for-player player disp-vec))))
