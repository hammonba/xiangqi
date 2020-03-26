(ns xiangqi.pedestal.websockets
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [medley.core :as medley])
  (:import [org.eclipse.jetty.websocket.api Session]))

(defmulti websocket-ontext (fn [_ msg] (or (:msg-type msg) (:action msg))))
(defmethod websocket-ontext :default
  [_ msg]
  (log/infof "websocket: %s" msg)
  msg)

(defn on-connect
  [ws-clients]
  (fn on-connect*
      [ws-session send-ch]
      (swap! ws-clients assoc ws-session send-ch)))

(defn sendmsg-async
  [sendch msg]
  (log/info "sendmsg-async: " msg)
  (async/go (async/>! sendch msg)))

(defn on-text
  [sendch msg]
  (log/info :msg "TEXT Message!" msg)
  (let [{:keys [correlation-id] :as req} (clojure.edn/read-string msg)]
    (let [resp (websocket-ontext sendch req)]
      (sendmsg-async sendch
        (pr-str (medley/assoc-some resp
                  :correlation-id correlation-id))))))

(defn on-binary
  [sendch payload offset length]
  (log/info :msg "Binary Message!" :bytes payload))

(defn on-error
  [sendch t]
  (log/error :msg "WE Error happened" :exception t))

(defn on-close
  [sendch num-code reason-text]
  (log/info :msg "WS Closed:" :reason reason-text))

(defn send-message-to-all!
  [ws-clients]
  (fn [message]
      (doseq [[^Session session channel] @ws-clients]
        (when (.isOpen session)
          (async/put! channel message)))))
