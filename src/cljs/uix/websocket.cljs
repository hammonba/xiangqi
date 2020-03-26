(ns uix.websocket
  (:require
   [cljs.core.async :as core.async :refer [go <! >!]]
   [haslett.client :as ws]
   [haslett.format :as ws-fmt]
   [xframe.core.alpha :as xf :refer [<sub]]))

(defonce correlation-counter (volatile! 0))
(defn next-correlation-id
  []
  (vswap! correlation-counter inc))

(def outstanding-responses (atom {}))

(defn single-request
  "adds a correlation id and dispatches a single request to the server
  returns a promise channel"
  [msg]
  (let [correlation-id (next-correlation-id)
        ch-resp (core.async/promise-chan)
        req (assoc msg :correlation-id correlation-id)]
    (swap! outstanding-responses
      assoc
      correlation-id
      {:correlation-id correlation-id
       :chan ch-resp
       :single-use true
       :req req})
    (go
      (let [{:keys [sink]} (<! (get-in @xf/db [::socket :ws-conn]))]
        (>! sink req)))

    ch-resp))

(defn dispatch-response
  [{:keys [correlation-id] :as msg}]
  (when correlation-id
    (when-let [{:keys [single-use chan]} (get @outstanding-responses correlation-id)]
      (go (>! chan msg)
        (when single-use
          (swap! outstanding-responses dissoc correlation-id)
          msg)))))

(defn dispatch-msg
  "process an incoming message from the server"
  [{:keys [msg-type] :as msg}]
  (let [ch (keyword "msg" (name msg-type))]
    (xf/dispatch [ch msg])))

(defn connect-websocket
  [url]
  (ws/connect url {:format ws-fmt/edn}))


(defn read-loop
  [ws-conn]
  (go (let [{:keys [close-status source] :as cc} (<! ws-conn)]
        (loop [new-msg (<! source)]
          (if (nil? new-msg)
            (xf/dispatch [::websocket-closed (<! close-status)])
            (do
              (dispatch-response new-msg)
              (recur (<! source))))))))

(xf/reg-event-fx ::send-async-msg
  (fn [db [_ msg]]
    {::send-to-websocket msg}))

(xf/reg-event-fx ::websocket-read
  (fn [db [_ msg]]
    (.log js/console ::websocket-read msg)))

(xf/reg-event-fx ::websocket-closed
  (fn [db [_ close-status]]
    (.log js/console ::websocket-closed close-status)))

(xf/reg-fx ::send-to-websocket
  (fn [db [_ msg]]
    (let [{:keys [sink]} (<! (get-in db [::websocket :ws-conn]))]
      (go (>! sink msg)))))

(xf/reg-event-db ::db-init
  (fn [db _]
    (assoc db ::websocket {:url "ws://localhost:8080/ws"})))

(xf/reg-event-db ::start-websocket
  (fn [db _]
    (when (nil? (get-in db [::socket :ws-conn]))
      (let [ws-conn (connect-websocket (get-in db [::websocket :url]))]
        (go (>! (:sink (<! ws-conn)) "HELO"))
        (update db ::socket assoc
          :ws-conn ws-conn
          :read-loop (read-loop ws-conn))))))

(defonce init-db
  (xf/dispatch [::db-init]))

(defn create-websocket
  []
  (xf/dispatch [::start-websocket]))
