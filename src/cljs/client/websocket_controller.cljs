(ns client.websocket-controller
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :as a :refer [<! >!]]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [citrus.core :as citrus]))

(defmulti control-ws (fn [event] (:action event event)))

(defmulti compute-dispatch (fn [{:keys [msg-type] :as new-msg}] msg-type))

(defn websocket-readloop
  "expected to be called as an effect.
   Waits for websocket connection and then puts incoming messages onto the events queue
   until the source channnel closes"
  [reconciler controller conn-chan]

  (go (let [{:keys [socket close-status source sink] :as cc} (<! conn-chan)]
        (loop [new-msg (<! source)]
          (when new-msg
            (apply citrus/dispatch! reconciler (compute-dispatch new-msg))
            (recur (<! source)))))))

(defmethod control-ws :init
  [_]
  (let [ws  (ws/connect "ws://localhost:8080/ws" {:format fmt/edn})]
    {:state {:ws ws}
     :websocket-readloop ws}))

(defn ws-post
  [{:keys [ws] :as state} msg]
  (go (>! (:sink (<! ws)) msg)))

(defmethod control-ws :get-board
  [event [args & _] {:keys [ws] :as state} cofx]
  (go (ws-post state args))
  (assoc state :activity :waiting))

(defmethod compute-dispatch :board
  [msg]
  [:board-controller :received-board msg])