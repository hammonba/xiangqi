(ns xiangqi.httpd
  "Xiangqi webserver for websockets and boards"
  (:require
   [compojure.core :as compojure :refer [GET]]
   [ring.middleware.params :as params]
   [compojure.route :as route]
   [aleph.http :as http]
   [byte-streams :as bs]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.bus :as bus]
   [clojure.core.async :as a]
   [clojure.edn :as edn]))


(def non-websocket-request
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected a websocket request."})

(defn echo-processfn
  [args]
  (clojure.pprint/pprint args)
  args)

(def initial-disposition
  [{:x 0, :y 6, :piecename "red-soldier" :moves [{:x "0" :y "6"}]}
   {:x 2, :y 6, :piecename "red-soldier"}
   {:x 4, :y 6, :piecename "red-soldier"}
   {:x 6, :y 6, :piecename "red-soldier"}
   {:x 8, :y 6, :piecename "red-soldier"}
   {:x 1, :y 7 :piecename "red-cannon" :moves [{:x 1 :y 8} {:x 1 :y 6} {:x 1 :y 5} {:x 1 :y 4} {:x 1 :y 3} {:x 1 :y 0}]}
   {:x 7, :y 7, :piecename "red-cannon"}
   {:x 0, :y 9, :piecename "red-chariot"}
   {:x 8, :y 9, :piecename "red-chariot"}
   {:x 1, :y 9, :piecename "red-horse"}
   {:x 7, :y 9, :piecename "red-horse"}

   {:x 2, :y 9, :piecename "red-minister"}
   {:x 6, :y 9, :piecename "red-minister"}
   {:x 3, :y 9, :piecename "red-guard"}
   {:x 5, :y 9, :piecename "red-guard"}
   {:x 4, :y 9, :piecename "red-marshal"}

   {:x 0, :y 3, :piecename "black-private"}
   {:x 2, :y 3, :piecename "black-private"}
   {:x 4, :y 3, :piecename "black-private"}
   {:x 6, :y 3, :piecename "black-private"}
   {:x 8, :y 3, :piecename "black-private"}

   {:x 1, :y 2, :piecename "black-catapult"}
   {:x 7, :y 2, :piecename "black-catapult"}
   {:x 0, :y 0, :piecename "black-chariot"}
   {:x 8, :y 0, :piecename "black-chariot"}

   {:x 1, :y 0, :piecename "black-horse"}
   {:x 7, :y 0, :piecename "black-horse"}

   {:x 2, :y 0, :piecename "black-elephant"}
   {:x 6, :y 0, :piecename "black-elephant"}

   {:x 3, :y 0, :piecename "black-guard"}
   {:x 5, :y 0, :piecename "black-guard"}
   {:x 4, :y 0, :piecename "black-general"}])

(defmulti process-msg (fn [{:keys [action] :as msg}] action))
(defmethod process-msg :get-board
  [{:keys [board-id] :as msg}]
  (clojure.pprint/pprint msg)
  {:disposition initial-disposition
   :board-id board-id
   :msg-type :board})

(defmethod process-msg :default
  [msg]
  (clojure.pprint/pprint "DEFAULT")
  (clojure.pprint/pprint msg)
  msg)

(defn ws-handler
  "This is another asynchronous handler, but uses `let-flow` instead of `chain` to define the
   handler in a way that at least somewhat resembles the synchronous handler."
  [req]
  (->
    (d/let-flow [socket (http/websocket-connection req)]
      (s/connect (s/map (comp pr-str process-msg edn/read-string) socket) socket))
    (d/catch
     (fn [_]
         non-websocket-request))))

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/ws" [] ws-handler)
      (route/not-found "No such page."))))


(def s (http/start-server handler {:port 10000}))

(comment
  (let [conn @(http/websocket-client "ws://localhost:10000/echo")]

    (s/put-all! conn
      (->> 10 range (map str)))

    (->> conn
      (s/transform (take 10))
      s/stream->seq
      doall))
  =>
  ("0" "1" "2" "3" "4" "5" "6" "7" "8" "9"))