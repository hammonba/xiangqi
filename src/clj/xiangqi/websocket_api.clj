(ns xiangqi.websocket-api
  (:require [integrant.core :as ig]
            [io.pedestal.http.jetty.websockets :as jetty.websockets]
            [io.pedestal.log :as log]
            [medley.core :as medley]
            [clojure.core.async :as async]
            [xiangqi.board-api :as board-api])
  (:import [org.eclipse.jetty.servlet ServletContextHandler$Context ServletContextHandler]
           [org.eclipse.jetty.websocket.api Session]
           [org.eclipse.jetty.websocket.servlet ServletUpgradeResponse ServletUpgradeRequest]))

(defmulti websocket-ontext
  (fn [conn config msg] (or (:msg-type msg) (:action msg))))

(defmethod websocket-ontext :default
  [_ _ msg]
  (log/info :websocket-ontext msg)
  msg)

(defmethod websocket-ontext :get-board
  [_ _ {:keys [board-ident]}]
  (xiangqi.board-api/describe board-ident))

(defrecord Conn [key ^Session ws-session send-ch])

(defn on-connect
  [key ^Session ws-session send-ch]
  (->Conn key ws-session send-ch))

(defn on-close
  [^Conn conn status-code reason]
  (log/info :fn :on-close :key (:key conn) :status-code status-code :reason reason)
  nil)

(defn on-error
  [^Conn conn cause]
  (log/warn :fn :on-error :key (:key conn) :exception cause)
  conn)

(defn sendmsg-async
  [conn msg]
  (log/info "sendmsg-async: " msg)
  (async/go (async/>! (:send-ch conn) msg)))

(defn on-text
  [^Conn conn component msg]
  (log/info :fn :on-text :key (:key conn) :msg msg)
  (let [{:keys [correlation-id] :as req} (clojure.edn/read-string msg)
        resp (websocket-ontext conn component req)]

    (sendmsg-async
      conn
      (pr-str (medley/assoc-some resp
                :correlation-id correlation-id))))
  conn)

(defn on-binary
  [^Conn conn payload offset length]
  (log/info :fn :on-binary :key (:key conn) :offset offset :length length)
  conn)

(defn websocket-routes
  [{:keys [ws-sessions] :as component}]
  {"/ws"
   (fn []
       (let [key (Object.)]
         {:on-connect (jetty.websockets/start-ws-connection
                        (fn [ws-session ch]
                            (swap! ws-sessions assoc key (on-connect key ws-session ch))))
          :on-text (fn [msg]
                       (swap! ws-sessions update key on-text component msg))
          :on-binary (fn [payload offset length]
                         (swap! ws-sessions update key on-binary payload offset length))
          :on-error (fn [cause]
                        (swap! ws-sessions update key on-error cause))
          :on-close (fn [status-code reason]
                        (on-close (get @ws-sessions key) status-code reason)
                        (swap! ws-sessions dissoc key))}))})

(defn ws-stateful-listener
  [^ServletUpgradeRequest req
   ^ServletUpgradeResponse resp
   wsmap-genfn]
  (jetty.websockets/make-ws-listener (wsmap-genfn)))

(defn build-context-configurator-addin
  "takes an atom with which to store websocket state updates
   an a ServletContextHandler to add ourselves in to "
  [component ^ServletContextHandler sch]
  (jetty.websockets/add-ws-endpoints
    sch
    (websocket-routes component)
    {:listener-fn ws-stateful-listener}))

(defmethod ig/init-key :component/websocket
  [_ config]
  (let [ws-sessions (atom {})
        config (assoc config :ws-sessions ws-sessions)]
    (assoc-in config
      [:add-ins
       :io.pedestal.http/container-options
       :context-configurator]
      #(build-context-configurator-addin config %)))
  )
