(ns xiangqi.pedestal.websockets
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async])
  (:import [org.eclipse.jetty.websocket.api Session]))

(defmulti websocket-ontext (fn [_ msg] (or (:msg-type msg) (:action msg))))

(defn on-connect
  [ws-clients]
  (fn on-connect*
      [ws-session send-ch]
      (swap! ws-clients assoc ws-session send-ch)))

(defn sendmsg-async
  [sendch msg]
  (async/go (async/>! sendch msg)))

(defn on-text
  [sendch msg]
  (log/info :msg (class msg))
  (log/info :msg (clojure.edn/read-string msg))
  (when-let [msg (websocket-ontext sendch (clojure.edn/read-string msg))]
    (clojure.pprint/pprint msg)
    (sendmsg-async sendch msg)))

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