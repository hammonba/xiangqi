(ns xiangqi.pedestal.board
  "simple interceptor to quickly output xiangqi boards"
  (:require [clojure.tools.logging :as log]
            [hiccup.core :as hiccup]
            [io.pedestal.http.route :as http.route]
            [medley.core :as medley]
            [ring.middleware.content-type]
            [ring.util.response :as ring-resp]
            [xiangqi.board.bit-utils :as bit-utils]
            [xiangqi.board.board-ident :as board-ident]
            [xiangqi.board-layout :as board-layout]
            [xiangqi.board-service :as board-service]
            [xiangqi.utils :as utils]
            [clojure.tools.logging :as clog]))


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
  [:a {:href (http.route/url-for :board.v1/html
               :path-params {:board-ident board-ident}
               :query-params {:open (utils/nname location)})}
   svg])

(defn complete-move
  [svg opened-move {:keys [board-after]}]
  [:a {:href (http.route/url-for :board.v1/html
               :path-params {:board-ident board-after})}
   svg])

(defn did-piece-open-move?
  [opened-move {:piece/keys [location] :as piece}]
  (when opened-move
    (= opened-move location)))

(def aom (atom nil))

(defn place-pieces
  [{:board/keys [disposition ident-string]} opened-move]
  (reset! aom opened-move)
  (board-layout/layout-pieces
    {:openmove-fn #(open-move ident-string %1 %2)
     :completemove-fn complete-move
     :did-piece-open-move?-fn #(do
                                 (clog/warn "didpiece-open-move %s, %s" opened-move %1)
                                 (clog/spy :warn
                                   (did-piece-open-move? opened-move %1)))}
    disposition
    (:disposition/location opened-move)
    (mapcat :end-locations (:end-locations (clog/spy :warn opened-move)))))


(def boundary-class
  {:player/red "red-boundary"
   :player/black "black-boundary"})

(defn layout-board
  [{:board/keys [opened-move] :as board-data}]
  (->>
    (place-pieces board-data (:disposition/location opened-move))
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
