(ns xiangqi.pedestal.board
  "simple interceptor to quickly output xiangqi boards"
  (:require [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [medley.core :as medley]
            [ring.middleware.content-type]
            [ring.util.response :as ring-resp]
            [xiangqi.board.bit-utils :as bit-utils]
            [xiangqi.board.board-ident :as board-ident]
            [xiangqi.board-layout :as board-layout]
            [xiangqi.board-service :as board-service]
            [xiangqi.utils :as utils]))


(defn get-boardident
  [request]
  (or (bit-utils/decodestring-into-bigint
        (get-in request [:path-params :board-ident]))
    board-ident/initial-board-218))

(defn get-openedmove
  [request]
  (when-let [loc (get-in request [:query-params :open])]
    (keyword "location" loc)))

(defn open-move
  [board-ident svg {:piece/keys [location]}]
  [:a {:href (str "/board/" board-ident "?open=" (utils/nname location))}
   svg])

(defn complete-move
  [svg opened-move {:keys [next-board]}]
  [:a {:href (str "/board/" (bit-utils/encodebigint-as-string next-board))}
   svg])

(defn did-piece-open-move?
  [opened-move {:keys [location] :as piece}]
  (when opened-move
    (= opened-move location)))

(defn place-pieces
  [{:board/keys [disposition ident]} opened-move]
  (board-layout/layout-pieces
    {:openmove-fn #(open-move ident %1 %2)
     :completemove-fn complete-move
     :did-piece-open-move?-fn #(did-piece-open-move? opened-move %1)}
    disposition
    opened-move
    (mapcat :end-locations (:end-locations opened-move))))

(defn layout-board
  [{:board/keys [opened-move] :as board-data}]
  (->>
    (place-pieces board-data opened-move)
    (board-layout/board-hiccup board-data)
    (vector :svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 12"})))

(defn find-opened-move
  [{:board/keys [disp-vec opened-move-location] :as board} ]
  (medley/assoc-some board
    :board/opened-move
    (first (filter #(= opened-move-location (:disposition/location %)) disp-vec))))

(defn stylesheet-link
  [href]
  [:link {:href href :rel "stylesheet" :type "text/css"}])

(defn add-enclosing-html
  [& body]
  [:html
   [:head (stylesheet-link "/css/style.css")]
   (into [:body] body)])

(defn board-interceptor
  [request]
  (-> request
    get-boardident
    board-service/board-by-ident
    (update :board/ident bit-utils/encodebigint-as-string)
    board-service/transform-for-websocket
    (assoc :board/opened-move-location (get-openedmove request))
    find-opened-move
    layout-board
    add-enclosing-html
    hiccup/html
    ring-resp/response
    (ring-resp/content-type "text/html")))
