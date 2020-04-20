(ns uix.websocket
  (:require
   [cljs.core.async :as core.async :refer [go go-loop <! >! close!]]
   [haslett.client :as ws]
   [haslett.format :as ws-fmt]
   [xframe.core.alpha :as xf :refer [<sub]]
   [clojure.core.async :as async]))

(def dbk
  "our subsection of the global state"
  :uix/websocket)

#_(defonce correlation-counter (volatile! 0))
#_(defn next-correlation-id
  []
  (vswap! correlation-counter inc))

#_(def outstanding-responses (atom {}))

#_(defn single-request
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
      (let [sink (get-in @xf/db [:uix/websocket ::ws-conn :sink])]
        (>! sink req)))

    ch-resp))

#_(defn dispatch-response
  [{:keys [correlation-id] :as msg}]
  (when correlation-id
    (when-let [{:keys [single-use chan]} (get @outstanding-responses correlation-id)]
      (go (>! chan msg)
        (when single-use
          (do
            (close! chan)
            (swap! outstanding-responses dissoc correlation-id))
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
  [{:keys [close-status source] :as ws-conn}]
  (go-loop [new-msg (<! source)]
    (if (nil? new-msg)
      (xf/dispatch [::websocket-closed (<! close-status)])
      (do
        #_(dispatch-response new-msg)
        (xf/dispatch [::read new-msg])
        (recur (<! source))))))

(defn timestamp-now [] (.getTime (js/Date.)))

#_(defn heartbeat-loop
  [{:keys [close-status] :as ws-conn}]
  (go-loop [action :donext]
    (when (= :donext action)
      (let [{:keys [server-time] :as resp}
            (<! (single-request
                  {:action :ping
                   :client-time (.getTime (js/Date.))}))]
        (.log js/console "heartbeat" resp))
      (recur (async/alt!
               (async/timeout 20000) :donext
               close-status :closed
               )))))

(xf/reg-event-db ::heartbeat-received
  (fn [db [_ {:keys [req]}]]
    (let [elapsed (- (timestamp-now) (:client-time req))]
      (update-in db [:uix/websocket ::latency] #(conj (take-last 100 %) elapsed)))))

(xf/reg-event-db ::heartbeat-failed
  (fn [db [_ resp]]
    (.log js/console "heartbeat-failed: " resp)
    (update-in db [:uix/websocket ::latency] #(conj (take-last 100 %) :failed))))

(defn heartbeat-loop
  [{:keys [close-status]}]
  (go-loop [action :recur]
    (when (= :recur action)
      (xf/dispatch [::send {:msg {:action :ping :client-time (timestamp-now)}
                            :on-ok ::heartbeat-received
                            :on-failed ::heartbeat-failed}])
      (recur (async/alt!
               (async/timeout 3000) :recur
               close-status :closed
               )))))

(xf/reg-event-fx ::send
  (fn [db [_ {:keys [msg single-use? on-ok on-failed]}]]
    (let [correlation-id (swap! (get-in db [:uix/websocket ::correlation-counter]) inc)
          req (assoc msg :correlation-id correlation-id)]
      {::raw-send! req
       :db (assoc-in db [:uix/websocket ::inflight-requests correlation-id]
             {:correlation-id correlation-id
              :single-use? (not (false? single-use?))
              :req req
              :on-ok on-ok
              :on-failed on-failed})})))

(xf/reg-event-fx ::read
  (fn [db [_ {:keys [correlation-id] :as msg}]]
    (or
      (when correlation-id
        (when-let [d (get-in db [:uix/websocket ::inflight-requests correlation-id])]
          (let [d2 (assoc d :resp msg)
                db2 (if (:single-use? d)
                      (update-in db [:uix/websocket ::inflight-requests] dissoc correlation-id)
                      (assoc-in db [:uix/websocket ::inflight-requests correlation-id] d2))]
            {:db db2
             :dispatch [(:on-ok d2) d2]}
            )))
      (do
        (.warn js/console "IGNORED READ " msg)
        {:db db}))))

(xf/reg-event-db ::websocket-closed
  (fn [db [_ close-status]]
    (update db :uix/websocket assoc
      ::state :closed
      :close-status close-status)))

(xf/reg-fx ::raw-send!
  (fn [db [_ msg]]
    (let [sink (get-in db [:uix/websocket ::ws-conn :sink])]
      (go (>! sink msg)))))

(xf/reg-event-db ::db-init
  (fn [db _]
    (assoc db :uix/websocket
              {::url "ws://localhost:8080/ws"
               ::correlation-counter (atom 0)
               ::inflight-requests {}
               ::state :not-started})))

(xf/reg-fx ::dispatch-or-queue
  (fn [db [_ evt]]
    (if (= :connected (get-in db [dbk ::state]))
      (xf/dispatch evt)
      (xf/dispatch [::queue-event evt]))))

(def conj-vec (fnil conj []))
(xf/reg-event-db ::queue-event
  (fn [db [_ evt]]
    (update-in db [dbk ::queued-events] conj-vec evt)))


(xf/reg-event-fx ::store-websocket
  (fn [db [_ ws-conn]]
    {:db (-> db
           (update :uix/websocket assoc
             ::ws-conn ws-conn
             ::read-loop (read-loop ws-conn)
             ;::heartbeat-loop (heartbeat-loop ws-conn)
             ::heartbeat-loop (heartbeat-loop ws-conn)
             ::state :connected)
           (update :uix/websocket dissoc ::queued-events))
     :dispatch-n (get-in db [dbk ::queued-events])}))

(xf/reg-event-db ::connect-websocket
  (fn [db _]
    (if (= :not-started (get-in db [:uix/websocket ::state]))
      (let [ws-conn (connect-websocket (get-in db [:uix/websocket ::url]))]
        (assoc db
          :connecting-chan
          (go (xf/dispatch [::store-websocket (<! ws-conn)])))
        )
      db)))

(defonce init-db
  (xf/dispatch [::db-init]))

(defn create-websocket
  []
  (xf/dispatch [::connect-websocket]))
