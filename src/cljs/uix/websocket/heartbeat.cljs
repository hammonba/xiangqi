(ns uix.websocket.heartbeat
  "provides heartbeart implementation"
  (:require [cljs.core.async :as async :refer [go go-loop <! >! close!]]
            [xframe.core.alpha :as xf]))

(defn timestamp-now [] (.getTime (js/Date.)))

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
      (xf/dispatch [:uix.websocket/send
                    {:msg {:action :ping :client-time (timestamp-now)}
                     :on-ok ::heartbeat-received
                     :on-failed ::heartbeat-failed}])
      (recur (async/alt!
               (async/timeout 14900) :recur
               close-status :closed
               )))))
